(ns juku.service.paatos
  (:require [juku.db.yesql-patch :as sql]
            [clojure.string :as str]
            [common.string :as xstr]
            [juku.db.database :refer [db with-transaction]]
            [juku.db.coerce :as coerce]
            [juku.db.sql :as dml]
            [juku.service.hakemus :as h]
            [juku.service.hakemuskausi :as hk]
            [juku.service.avustuskohde :as ak]
            [juku.service.pdf :as pdf]
            [juku.service.organisaatio :as o]
            [juku.service.asiahallinta :as asha]
            [schema.coerce :as scoerce]
            [juku.schema.paatos :as s]
            [common.collection :as col]
            [slingshot.slingshot :as ss]
            [common.core :as c]
            [clojure.java.io :as io]
            [clj-time.core :as time]
            [clj-time.coerce :as timec]
            [juku.service.user :as user])
  (:import (org.joda.time LocalDate)
           (java.math RoundingMode)))

; *** Päätökseen liittyvät kyselyt ***
(sql/defqueries "paatos.sql")

; *** Päätösskeemaan liittyvät konversiot tietokannan tietotyypeistä ***
(def coerce-paatos (scoerce/coercer s/Paatos coerce/db-coercion-matcher))

; *** Päätökseen liittyvät poikkeustyypit ***
(derive ::paatos-not-found ::col/not-found)

(defn find-current-paatos [hakemusid]
  (let [paatos (first (map coerce-paatos (select-current-paatos {:hakemusid hakemusid})))]
    (or paatos {:hakemusid         hakemusid
                :paatosnumero      -1
                :myonnettyavustus  0
                :voimaantuloaika   nil
                :poistoaika        nil
                :paattaja          nil
                :paattajanimi      (:paattajanimi (first (select-latest-paattajanimi)))
                :selite            nil})))

(defn find-paatos [hakemusid paatosnumero]
  (first (map coerce-paatos (select-paatos {:hakemusid hakemusid :paatosnumero paatosnumero}))))

(defn new-paatos! [paatos] (insert-paatos! paatos))

(defn- assert-unique-update! [updateamount hakemusid]
  (if (> updateamount 1) (ss/throw+ (str "Tietokannan tiedot ovat virheelliset. Hakemuksella " hakemusid " on kaksi avointa päätöstä."))))

(defn assoc-paattajanimi [paatos] (update-in paatos [:paattajanimi] (fn [x] (if x x))))

(defn save-paatos! [paatos]
  (let [updated (update-paatos! (assoc-paattajanimi paatos))]
    (assert-unique-update! updated (:hakemusid paatos))
    (if (== updated 0) (new-paatos! (assoc-paattajanimi paatos)))
    nil))

(defn paatos-template [hakemus]
  (str "paatos-" (str/lower-case (:hakemustyyppitunnus hakemus)) "-2016.txt"))

(def maararahamomentti
  {"KS1"	"31.30.63.09"
   "KS2"	"31.30.63.11"})

(defn percentage [^BigDecimal x ^BigDecimal y]
  (.setScale ^BigDecimal (* (.divide x y 2 RoundingMode/HALF_UP) 100) 0))

(defn mh-template-values [hakemus mh-haettuavustus organisaatio]
  (let [{:keys [voimaantuloaika, myonnettyavustus]}
          (first (select-hakemus-paatos (assoc hakemus :hakemustyyppitunnus "AH0")))]
    {:ah0-paatospvm (or (some-> voimaantuloaika coerce/date->localdate h/format-date)
                        "<avustuksen myöntämispvm>")
     :ah0-myonnettyavustus (or myonnettyavustus
                               "<myönnetty avustus>")
     :osuusavustuksesta (or ((c/nil-safe percentage) mh-haettuavustus myonnettyavustus)
                            "<osuus avustuksesta>")
     :momentti (maararahamomentti (:lajitunnus organisaatio))}))

(defn mh2-templatevalues [hakemus]
  (let [{:keys [voimaantuloaika, myonnettyavustus]}
        (first (select-hakemus-paatos (assoc hakemus :hakemustyyppitunnus "MH1")))]
    {:mh1-paatospvm (or (some-> voimaantuloaika coerce/date->localdate h/format-date)
                        "<maksatuspäätös pvm>")
     :mh1-myonnettyavustus (or myonnettyavustus
                               "<maksettu avustus>")}))

(defn paatos-pdf
  ([hakemusid] (paatos-pdf hakemusid false))

  ([hakemusid preview]
    (let [paatos (find-current-paatos hakemusid)
          paatospvm-txt (if preview "<päätöspäivämäärä>" (h/format-date (time/today)))
          lahetyspvm-txt (some-> (select-lahetys-pvm {:hakemusid hakemusid})
                                 first :lahetyspvm coerce/date->localdate h/format-date)
          hakemus (h/get-hakemus+ hakemusid)
          organisaatio (o/find-organisaatio (:organisaatioid hakemus))
          avustuskohteet (ak/find-avustuskohteet-by-hakemusid hakemusid)
          hakuajat (hk/find-hakuajat (:vuosi hakemus))
          haettuavustus (ak/total-haettavaavustus avustuskohteet)

          template (slurp (io/reader (io/resource (str "pdf-sisalto/templates/" (paatos-template hakemus)))))
          common-template-values
            {:organisaatio-nimi (:nimi organisaatio)
             :organisaatiolaji-pl-gen (h/organisaatiolaji->plural-genetive (:lajitunnus organisaatio))
             :paatosspvm paatospvm-txt
             :lahetyspvm (or lahetyspvm-txt "<lähetyspäivämäärä>")
             :vuosi (:vuosi hakemus)
             :avustuskohteet (ak/avustuskohteet-section avustuskohteet)
             :haettuavustus haettuavustus
             :omarahoitus (ak/total-omarahoitus avustuskohteet)

             :selite (c/maybe-nil #(str "\n\n\t" %) "" (:selite paatos))
             :myonnettyavustus (:myonnettyavustus paatos)
             :mh1-hakuaika-loppupvm (h/format-date (get-in hakuajat [:mh1 :loppupvm]))
             :mh2-hakuaika-loppupvm (h/format-date (get-in hakuajat [:mh2 :loppupvm]))
             :paattaja (:paattajanimi paatos)
             :esittelija (user/user-fullname (user/find-user (:kasittelija hakemus)))}

          template-values
            (case (:hakemustyyppitunnus hakemus)
              "AH0" common-template-values
              "MH1" (merge common-template-values (mh-template-values hakemus haettuavustus organisaatio))
              "MH2" (merge common-template-values (mh-template-values hakemus haettuavustus organisaatio)
                                                  (mh2-templatevalues hakemus)))]

      (pdf/muodosta-pdf
        {:otsikko {:teksti "Valtionavustuspäätös" :paivays paatospvm-txt :diaarinumero (:diaarinumero hakemus)}
         :teksti (xstr/interpolate template template-values)

         :footer (str "Liikennevirasto" (if preview " - esikatselu"))}))))

(defn find-paatos-pdf [hakemusid]
  (let [paatos (find-current-paatos hakemusid)]
    (if (:voimaantuloaika paatos)
      (if-let [asiakirja (:asiakirja (first (select-latest-paatosasiakirja {:hakemusid hakemusid})))]
        (coerce/inputstream asiakirja)
        (ss/throw+ (str "Hakemuksen " hakemusid " päätösasiakirjaa ei löydy hakemustilahistoriasta.")))

      (paatos-pdf hakemusid true))))

(defn hyvaksy-paatos! [hakemusid]
  (with-transaction
    (let [hakemus (h/get-hakemus hakemusid)
          updated (update-paatos-hyvaksytty! {:hakemusid hakemusid})
          paatos-asiakirja (paatos-pdf hakemusid)]
      (assert-unique-update! updated hakemusid)
      (dml/assert-update updated {:type ::paatos-not-found
                                  :hakemusid hakemusid
                                  :message (str "Hakemuksella " hakemusid " ei ole avointa päätöstä")})
      (h/change-hakemustila+log! hakemus "P" "T" "päättäminen" paatos-asiakirja)
      (if-let [diaarinumero (:diaarinumero hakemus)]
        (asha/paatos diaarinumero {:paattaja (user/user-fullname user/*current-user*)} paatos-asiakirja))))
  nil)

(defn peruuta-paatos! [hakemusid]
  (with-transaction
    (let [hakemus (h/get-hakemus hakemusid)
          updated (update-paatos-hylatty! {:hakemusid hakemusid})]
      (assert-unique-update! updated hakemusid)
      (dml/assert-update updated {:type ::paatos-not-found
                                  :hakemusid hakemusid
                                  :message (str "Hakemuksella " hakemusid " ei ole voimassaolevaa päätöstä")})
      (h/change-hakemustila+log! hakemus "T" "P" "päätöksen peruuttaminen")))
  nil)