(ns juku.service.hakemus
  (:require [juku.db.yesql-patch :as sql]
            [juku.service.user :as user]
            [juku.db.database :refer [db with-transaction]]
            [juku.db.coerce :as coerce]
            [juku.db.sql :as dml]
            [juku.service.organisaatio :as o]
            [juku.service.asiahallinta :as asha]
            [juku.service.avustuskohde :as ak]
            [juku.service.hakemus-core :as h]
            [juku.service.email :as email]
            [slingshot.slingshot :as ss]
            [juku.service.liitteet :as l]
            [common.string :as xstr]
            [clojure.string :as str]
            [clj-time.core :as time]
            [clj-time.coerce :as timec]
            [schema.coerce :as scoerce]
            [clojure.java.io :as io]
            [juku.service.pdf :as pdf]
            [juku.schema.hakemus :refer :all]
            [ring.util.http-response :as r]
            [common.collection :as coll]
            [common.core :as c]
            [juku.service.hakemus-core :as hc]
            [juku.service.ely-hakemus :as ely])
  (:import (org.joda.time LocalDate)))

(defn get-hakemus-by-id! [hakemusid]
  (let [hakemus (h/get-hakemus+ hakemusid)
        diaarinumero (:diaarinumero hakemus)]
    (if (and (user/has-privilege* :kasittely-hakemus)
             (nil? (:kasittelija hakemus))
             (c/not-nil? diaarinumero)
             (#{"V" "TV"} (:hakemustilatunnus hakemus)))
      (do
        (asha/kasittelyssa diaarinumero)
        (h/save-hakemus-kasittelija! hakemusid (:tunnus user/*current-user*))))
    hakemus))

;; *** Hakemusasiakirjan (pdf-dokumentti) tuottaminen ***

(defn hakemus-template [hakemus]
  (str "hakemus-" (str/lower-case (:hakemustyyppitunnus hakemus)) "-2016.txt"))

(defn common-template-values[esikatselu-message pvm hakemus]
  (let [organisaatio (o/find-organisaatio (:organisaatioid hakemus))]
    {:organisaatio-nimi (:nimi organisaatio)
     :organisaatiolaji-pl-gen (h/organisaatiolaji->plural-genetive (:lajitunnus organisaatio))
     :vireillepvm pvm
     :vuosi (:vuosi hakemus)
     :liitteet (l/liitteet-section (:id hakemus))
     :lahettaja (if esikatselu-message "<hakijan nimi, joka on lähettänyt hakemuksen>"
                                       (user/user-fullname user/*current-user*))}))

(defn hakemus-pdf
  ([hakemus] (hakemus-pdf hakemus nil))
  ([hakemus esikatselu-message]
    (let [pvm (h/format-date (time/today))
          hakemusid (:id hakemus)
          template (slurp (io/reader (io/resource (str "pdf-sisalto/templates/" (hakemus-template hakemus)))))
          common-template-values (common-template-values esikatselu-message pvm hakemus)
          template-values
            (case (:hakemustyyppitunnus hakemus)
              "ELY" (merge common-template-values (ely/ely-template-values hakemus))
              (merge common-template-values (ak/avustuskohde-template-values-by-hakemusid hakemusid)))]

      (pdf/muodosta-pdf
        {:otsikko {:teksti "Valtionavustushakemus" :paivays pvm :diaarinumero (:diaarinumero hakemus)}
         :teksti (xstr/interpolate template template-values)
         :footer (c/maybe-nil #(str "Liikennevirasto - esikatselu - " %) "Liikennevirasto" esikatselu-message)}))))

(defn find-hakemus-pdf [hakemusid]
  (let [hakemus (h/get-hakemus hakemusid)]
    (case (:hakemustilatunnus hakemus)
      "0" (hakemus-pdf hakemus (str "hakuaika ei ole alkanut"))
      "K" (hakemus-pdf hakemus (str "hakemus on keskeneräinen"))
      "T0" (hakemus-pdf hakemus (str "hakemus on täydennettävänä"))
      (if-let [asiakirja (:asiakirja (first (h/select-latest-hakemusasiakirja {:hakemusid hakemusid})))]
        (coerce/inputstream asiakirja)
        (ss/throw+ (str "Hakemuksen " hakemusid " asiakirjaa ei löydy hakemustilahistoriasta."))))))

;; *** Hakemustilan käsittely ***

(defn assert-oma-hakemus*! [hakemus]
  (when-not (hc/is-hakemus-owner?* hakemus)
    (hc/throw! r/forbidden
               (str "Käyttäjällä " (:tunnus user/*current-user*)
                    " ei ole oikeutta lähettaa hakemusta: " (:id hakemus) ". "
                    "Ainoastaan oman hakemuksen saa lähettää."))))

(def hakemustyyppi->asiatyyppi
  {"AH0" "AH"
   "ELY" "ELY"})

(defn laheta-hakemus! [hakemusid]
  (with-transaction
    (let [hakemus (h/get-hakemus+ hakemusid)
          hakemuskausi (h/find-hakemuskausi hakemus)
          hakemus-asiakirja (hakemus-pdf hakemus)
          organisaatio (o/find-organisaatio (:organisaatioid hakemus))

          liitteet (l/find-liitteet+sisalto hakemusid)]

      (assert-oma-hakemus*! hakemus)
      (h/change-hakemustila! hakemus "V" ["K"] "vireillelaitto")

      (if (#{"AH0" "ELY"} (:hakemustyyppitunnus hakemus))
        (h/update-hakemus-set-diaarinumero!
          {:vuosi (:vuosi hakemus)
           :organisaatioid (:organisaatioid hakemus)
           :diaarinumero (asha/hakemus-vireille {:kausi (:diaarinumero hakemuskausi)
                                                 :tyyppi (hakemustyyppi->asiatyyppi (:hakemustyyppitunnus hakemus))
                                                 :hakija (:nimi organisaatio)}
                                                hakemus-asiakirja liitteet)})

        (if-let [diaarinumero (:diaarinumero hakemus)]
          (let [kasittelija (user/find-user (or (:kasittelija hakemus)
                                                (:kasittelija (first (h/select-avustushakemus-kasittelija hakemus)))
                                                (:luontitunnus hakemus)))]
            (asha/maksatushakemus diaarinumero
                                  {:kasittelija (user/user-fullname kasittelija)
                                   :lahettaja (:nimi organisaatio)}
                                  hakemus-asiakirja liitteet))))

      (h/insert-hakemustila-event+asiakirja! {:hakemusid hakemusid
                                            :hakemustilatunnus "V"
                                            :asiakirja (hakemus-pdf (h/get-hakemus hakemusid))})

      (email/send-hakemustapahtuma-message hakemus "V" nil)))
  nil)

(defn tarkasta-hakemus! [hakemusid]
  (with-transaction
    (let [hakemus (h/get-hakemus hakemusid)]
      (h/change-hakemustila+log! hakemus "T" ["V" "TV"] "tarkastaminen")
      (h/save-hakemus-kasittelija! hakemusid (:tunnus user/*current-user*))
      (if-let [diaarinumero (:diaarinumero hakemus)]
        (asha/tarkastettu diaarinumero))))
  nil)

(defn maarapvm [loppupvm]
  (time/latest [loppupvm (time/plus (time/today) (time/days 14))]))

(defn add-taydennyspyynto! [hakemusid maarapaiva selite]
  (h/insert-taydennyspyynto! {:hakemusid hakemusid :maarapvm maarapaiva :selite selite}))

(defn taydennyspyynto!
  ([hakemusid] (taydennyspyynto! hakemusid nil))
  ([hakemusid selite]
  (with-transaction
    (let [hakemus (h/get-hakemus hakemusid)
          maarapvm (maarapvm (get-in hakemus [:hakuaika :loppupvm]))
          kasittelija user/*current-user*
          organisaatio (o/find-organisaatio (:organisaatioid hakemus))]

      (h/change-hakemustila+log! hakemus "T0" ["V" "TV"] "täydennyspyyntö")

      (add-taydennyspyynto! hakemusid maarapvm selite)
      (if-let [diaarinumero (:diaarinumero hakemus)]
        (asha/taydennyspyynto diaarinumero
                              {:maaraaika   (time/from-time-zone (timec/to-date-time maarapvm) (time/default-time-zone))
                               :kasittelija (user/user-fullname kasittelija)
                               :hakija      (:nimi organisaatio)}))

      (email/send-hakemustapahtuma-message hakemus "T0" nil)))
  nil))

(defn laheta-taydennys! [hakemusid]
  (with-transaction
    (let [hakemus (h/get-hakemus+ hakemusid)
          hakemus-asiakirja-bytes (c/slurp-bytes (hakemus-pdf hakemus))
          organisaatio (o/find-organisaatio (:organisaatioid hakemus))
          kasittelija (user/find-user (or (:kasittelija hakemus) (:luontitunnus hakemus)))
          liitteet (l/find-liitteet+sisalto hakemusid)]

      (assert-oma-hakemus*! hakemus)
      (h/change-hakemustila+log! hakemus "TV" ["T0"] "täydentäminen" (io/input-stream hakemus-asiakirja-bytes))

      (if-let [diaarinumero (:diaarinumero hakemus)]
        (asha/taydennys diaarinumero
                        {:kasittelija (user/user-fullname kasittelija)
                         :lahettaja (:nimi organisaatio)}
                        (io/input-stream hakemus-asiakirja-bytes) liitteet))

      (email/send-hakemustapahtuma-message hakemus "TV" nil)))
  nil)