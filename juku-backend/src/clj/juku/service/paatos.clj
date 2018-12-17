(ns juku.service.paatos
  (:require [juku.db.yesql-patch :as sql]
            [clojure.string :as str]
            [common.string :as xstr]
            [juku.db.database :refer [db with-transaction]]
            [juku.db.coerce :as coerce]
            [juku.db.sql :as dml]
            [juku.service.hakemus-core :as h]
            [juku.service.hakemuskausi :as hk]
            [juku.service.avustuskohde :as ak]
            [juku.service.pdf :as pdf]
            [juku.service.organisaatio :as o]
            [juku.service.asiahallinta :as asha]
            [juku.service.email :as email]
            [schema.coerce :as scoerce]
            [juku.schema.paatos :as s]
            [common.collection :as col]
            [slingshot.slingshot :as ss]
            [common.core :as c]
            [clojure.java.io :as io]
            [clj-time.core :as time]
            [juku.service.user :as user]
            [juku.service.ely-hakemus :as ely]
            [juku.service.hakemus-core :as hc]))

; *** Päätökseen liittyvät kyselyt ***
(sql/defqueries "paatos.sql")

; *** Päätösskeemaan liittyvät konversiot tietokannan tietotyypeistä ***
(def coerce-paatos (scoerce/coercer s/Paatos coerce/db-coercion-matcher))

; *** Päätökseen liittyvät poikkeustyypit ***
(derive ::paatos-not-found ::col/not-found)

(defn assoc-paattajanimi [paatos]
  (assoc paatos :paattajanimi (c/maybe-nil (comp user/user-fullname user/find-user) nil
                                           (:paattaja paatos))))

(defn find-current-paatos [hakemusid]
  (let [paatos (first (map (comp coerce-paatos assoc-paattajanimi) (select-current-paatos {:hakemusid hakemusid})))]
    (or paatos {:hakemusid         hakemusid
                :paatosnumero      -1
                :myonnettyavustus  0
                :voimaantuloaika   nil
                :poistoaika        nil
                :paattaja          nil
                :paattajanimi      nil
                :selite            nil})))

(defn find-paatos [hakemusid paatosnumero]
  (first (map (comp coerce-paatos assoc-paattajanimi) (select-paatos {:hakemusid hakemusid :paatosnumero paatosnumero}))))

(defn new-paatos! [paatos] (insert-paatos! paatos))

(defn- assert-unique-update! [updateamount hakemusid]
  (if (> updateamount 1) (ss/throw+ (str "Tietokannan tiedot ovat virheelliset. Hakemuksella " hakemusid " on kaksi avointa päätöstä."))))

(defn save-paatos! [paatos]
  (let [updated (update-paatos! paatos)]
    (assert-unique-update! updated (:hakemusid paatos))
    (if (== updated 0) (new-paatos! paatos))
    nil))

(defn paatos-template [hakemus organisaatio]
  (case (:hakemustyyppitunnus hakemus)
    "AH0" (str "paatos-ah0-" (str/lower-case (:lajitunnus organisaatio)) "-2018.txt")
    "ELY" (case (:vuosi hakemus)
            2018 "paatos-ely-2018.txt"
            2017 "paatos-ely-2017.txt"
            "paatos-ely-2016.txt")
    (str "paatos-" (str/lower-case (:hakemustyyppitunnus hakemus)) "-2018.txt")))

(def maararahamomentti
  {"KS1"	"31.30.63.09"
   "KS2"	"31.30.63.11"})

(defn ^BigDecimal percentage [x y]
  (with-precision 2 :rounding HALF_UP (bigdec (* (/ x y) 100))))

(defn format-bigdec [^BigDecimal v] (-> v .stripTrailingZeros .toPlainString))

(defn mh-template-values [hakemus mh-haettuavustus organisaatio]
  (let [{:keys [voimaantuloaika, myonnettyavustus]}
          (first (select-hakemus-paatos (assoc hakemus :hakemustyyppitunnus "AH0")))]
    {:ah0-paatospvm (or (some-> voimaantuloaika coerce/date->localdate h/format-date)
                        "<avustuksen myöntämispvm>")
     :ah0-myonnettyavustus (or (pdf/format-number myonnettyavustus)
                               "<myönnetty avustus>")
     :osuusavustuksesta (or (when (some nil? [mh-haettuavustus, myonnettyavustus]) "<osuus avustuksesta>")
                            (when (zero? myonnettyavustus) "**")
                            ((comp format-bigdec percentage) mh-haettuavustus myonnettyavustus))
     :momentti (maararahamomentti (:lajitunnus organisaatio))}))

(defn mh2-templatevalues [hakemus]
  (let [{:keys [voimaantuloaika, myonnettyavustus]}
        (first (select-hakemus-paatos (assoc hakemus :hakemustyyppitunnus "MH1")))]
    {:mh1-paatospvm (or (some-> voimaantuloaika coerce/date->localdate h/format-date)
                        "<maksatuspäätös pvm>")
     :mh1-myonnettyavustus (or (pdf/format-number myonnettyavustus)
                               "<maksettu avustus>")}))

(defn alv-selite [avustuskohteet]
  (if (some ak/include-alv? (ak/filter-avustustahaettu avustuskohteet))
    "\n\n\tAvustukseen sisältyy arvonlisävero hintavelvoitteen korvaamisen osalta."
    ""))

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

          template (slurp (io/reader (io/resource (str "pdf-sisalto/templates/" (paatos-template hakemus organisaatio)))))
          common-template-values
            {:organisaatio-nimi (:nimi organisaatio)
             :organisaatiolaji-pl-gen (h/organisaatiolaji->plural-genetive (:lajitunnus organisaatio))
             :paatosspvm paatospvm-txt
             :lahetyspvm (or lahetyspvm-txt "<lähetyspäivämäärä>")
             :vuosi (:vuosi hakemus)

             :selite (c/maybe-nil #(str "\n\n\t" (str/trim (str/replace % #"\R+" "\n\n\t"))) ""
                                  (c/nil-if str/blank? (:selite paatos)))

             :myonnettyavustus (pdf/format-number (:myonnettyavustus paatos))
             :mh1-hakuaika-loppupvm (h/format-date (get-in hakuajat [:mh1 :loppupvm]))
             :mh2-hakuaika-loppupvm (h/format-date (get-in hakuajat [:mh2 :loppupvm]))
             :paattaja (:paattajanimi paatos)
             :esittelija (user/user-fullname (user/find-user (:kasittelija hakemus)))}

          template-values
            (case (:hakemustyyppitunnus hakemus)
              "AH0" (merge common-template-values
                           {:alv-selite (alv-selite avustuskohteet)}
                           (ak/avustuskohde-template-values avustuskohteet))
              "MH1" (merge common-template-values
                           (ak/avustuskohde-template-values avustuskohteet)
                           (mh-template-values hakemus haettuavustus organisaatio))
              "MH2" (merge common-template-values
                           (ak/avustuskohde-template-values avustuskohteet)
                           (mh-template-values hakemus haettuavustus organisaatio)
                           (mh2-templatevalues hakemus))
              "ELY" (merge common-template-values
                           (ely/ely-paatos-template-values paatos hakemus)))
          otsikko
            (case (:hakemustyyppitunnus hakemus)
              "ELY" "Päätös"
              "Valtionavustuspäätös")]

      (pdf/muodosta-pdf
        {:otsikko {:teksti otsikko :paivays paatospvm-txt :diaarinumero (:diaarinumero hakemus)}
         :teksti (xstr/interpolate template template-values)

         :footer (str "Liikennevirasto" (if preview " - esikatselu"))}))))

(defn find-paatos-pdf [hakemusid]
  (let [paatos (find-current-paatos hakemusid)]
    (if (:voimaantuloaika paatos)
      (if-let [asiakirja (:asiakirja (first (select-latest-paatosasiakirja {:hakemusid hakemusid})))]
        (coerce/inputstream asiakirja)
        (ss/throw+ (str "Hakemuksen " hakemusid " päätösasiakirjaa ei löydy hakemustilahistoriasta.")))

      (paatos-pdf hakemusid true))))

(defn hyvaksy-paatos!
  ([hakemusid] (hyvaksy-paatos! hakemusid true))
  ([hakemusid enableAsiahallinta?]
    (with-transaction
      (let [hakemus (h/get-hakemus hakemusid)
            updated (update-paatos-hyvaksytty! {:hakemusid hakemusid})
            ^io/byte-array-type paatos-asiakirja-bytes (c/slurp-bytes (paatos-pdf hakemusid))]
        (assert-unique-update! updated hakemusid)
        (dml/assert-update updated {:type ::paatos-not-found
                                    :hakemusid hakemusid
                                    :message (str "Hakemuksella " hakemusid " ei ole avointa päätöstä")})
        (h/change-hakemustila+log! hakemus "P" "T" "päättäminen" (io/input-stream paatos-asiakirja-bytes))
        (when enableAsiahallinta?
          (if-let [diaarinumero (:diaarinumero hakemus)]
            (asha/paatos diaarinumero
                         {:paattaja (user/user-fullname user/*current-user*)}
                         (io/input-stream paatos-asiakirja-bytes))))

        (email/send-hakemustapahtuma-message hakemus "P" (io/input-stream paatos-asiakirja-bytes))))
    nil))

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

(defn hyvaksy-paatokset! [vuosi hakemustyyppitunnus enableAsiahallinta?]
  (with-transaction
    (doseq [hakemusid (hc/find-hakemukset-from-kausi vuosi hakemustyyppitunnus)]
      (hyvaksy-paatos! hakemusid enableAsiahallinta?))))

(defn save-paatokset! [paatokset]
  (with-transaction
    (doseq [paatos paatokset] (save-paatos! paatos))))

(defn find-any-ely-paatos [vuosi]
  (let [hakemusid (first (hc/find-hakemukset-from-kausi vuosi "ELY"))]
    (find-current-paatos hakemusid)))