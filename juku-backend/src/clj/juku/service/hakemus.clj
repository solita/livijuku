(ns juku.service.hakemus
  (:require [juku.db.yesql-patch :as sql]
            [juku.service.user :as user]
            [juku.db.database :refer [db with-transaction]]
            [juku.db.coerce :as coerce]
            [juku.db.sql :as dml]
            [juku.service.organisaatio :as o]
            [juku.service.asiahallinta :as asha]
            [juku.service.avustuskohde :as ak]
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
            [common.core :as c])
  (:import (org.joda.time LocalDate)))

; *** Hakemukseen liittyvät kyselyt ***
(sql/defqueries "hakemus.sql")

; *** Hakemus skeemaan liittyvät konversiot tietokannan tietotyypeistä ***
(def coerce-hakemus (scoerce/coercer Hakemus coerce/db-coercion-matcher))
(def coerce-hakemus+ (scoerce/coercer Hakemus+ coerce/db-coercion-matcher))
(def coerce-hakemus-suunnitelma (scoerce/coercer HakemusSuunnitelma coerce/db-coercion-matcher))

; *** Virheviestit tietokannan rajoitteista ***
(def constraint-errors
  {:hakemus_hakemustyyppi_fk {:http-response r/not-found :message "Hakemustyyppiä {hakemustyyppitunnus} ei ole olemassa."}
   :hakemus_organisaatio_fk {:http-response r/not-found :message "Hakemuksen organisaatiota {organisaatioid} ei ole olemassa."}
   :hakemus_kasittelija_fk {:http-response r/not-found :message "Hakemuksen käsittelijää {kasittelija} ei ole olemassa."}
   :hakemus_hakemuskausi_fk {:http-response r/not-found :message "Hakemuskautta {vuosi} ei ole olemassa."}})

; *** Hakemukseen liittyvät poikkeustyypit ***
(derive ::hakemus-not-found ::coll/not-found)

; *** Hakemukseen ja sen sisältöön liittyvät palvelut ***

(defn find-organisaation-hakemukset [organisaatioid]
  (map (comp coerce-hakemus coerce/row->object)
    (select-organisaation-hakemukset {:organisaatioid organisaatioid})))

(defn find-all-hakemukset []
  (map (comp coerce-hakemus coerce/row->object)
       (select-all-hakemukset)))

(defn hakemukset-group-by-hakemuskausi [hakemukset]
  (let [vuosittain (group-by :vuosi hakemukset)]
    (reduce (fn [result [key value]] (conj result {:vuosi key :hakemukset value}))
            '() vuosittain)))

(defn find-organisaation-hakemukset-vuosittain [organisaatioid]
  (hakemukset-group-by-hakemuskausi (find-organisaation-hakemukset organisaatioid)))

(defn find-kayttajan-hakemukset-vuosittain []
  (find-organisaation-hakemukset-vuosittain (:organisaatioid user/*current-user*)))

(defn find-hakemukset-vuosittain []
  (hakemukset-group-by-hakemuskausi (find-all-hakemukset)))

(defn- get-any-hakemus [hakemusid select coerce]
  (-> (select {:hakemusid hakemusid})
      (coll/single-result-required! {:type ::hakemus-not-found
                                     :hakemusid hakemusid
                                     :message (str "Hakemusta " hakemusid " ei ole olemassa.")})
      coerce/row->object
      coerce))

(defn get-hakemus+ [hakemusid]
  (let [hakemus (get-any-hakemus hakemusid select-hakemus+ coerce-hakemus+)]
    (if (= (:hakemustilatunnus hakemus) "T0")
      (coerce-hakemus+ (assoc hakemus :taydennyspyynto (first (select-latest-taydennyspyynto {:hakemusid hakemusid}))))
      hakemus)))

(defn get-hakemus [hakemusid] (get-any-hakemus hakemusid select-hakemus coerce-hakemus))

(defn find-hakemussuunnitelmat [vuosi hakemustyyppitunnus]
  (map (comp coerce-hakemus-suunnitelma coerce/row->object)
       (select-hakemussuunnitelmat {:vuosi vuosi :hakemustyyppitunnus hakemustyyppitunnus})))

(defn add-hakemus! [hakemus]
  (:id (dml/insert-with-id db "hakemus"
                           (-> hakemus
                               coerce/object->row
                               coerce/localdate->sql-date)
                           constraint-errors hakemus)))

(defn- update-hakemus-by-id [hakemus hakemusid]
  (dml/assert-update (dml/update-where-id db "hakemus" hakemus hakemusid)
                     {:type ::hakemus-not-found :message (str "Hakemusta " hakemusid " ei ole olemassa.")}))

(defn save-hakemus-selite! [hakemusid selite]
  (update-hakemus-by-id {:selite selite} hakemusid))

(defn save-hakemus-suunniteltuavustus! [hakemusid suunniteltuavustus]
  (update-hakemus-by-id {:suunniteltuavustus suunniteltuavustus} hakemusid))

(defn save-hakemus-kasittelija! [hakemusid kasittelija]
  (update-hakemus-by-id {:kasittelija kasittelija} hakemusid))

(defn get-hakemus-by-id! [hakemusid]
  (let [hakemus (get-hakemus+ hakemusid)
        diaarinumero (:diaarinumero hakemus)]
    (if (and (user/has-privilege* :kasittely-hakemus)
             (nil? (:kasittelija hakemus))
             (c/not-nil? diaarinumero)
             (#{"V" "TV"} (:hakemustilatunnus hakemus)))
      (do
        (asha/kasittelyssa diaarinumero)
        (save-hakemus-kasittelija! hakemusid (:tunnus user/*current-user*))))
    hakemus))

;; *** Hakemusasiakirjan (pdf-dokumentti) tuotattaminen ***

(def organisaatiolaji->plural-genetive
  {"KS1" "suurten kaupunkiseutujen",
   "KS2" "keskisuurten kaupunkiseutujen",
   "ELY" "ELY-keskusten"})

(defn hakemus-template [hakemus]
  (str "hakemus-" (str/lower-case (:hakemustyyppitunnus hakemus)) "-2016.txt"))

(defn ^String format-date [^LocalDate date]
  (.toString ^LocalDate date "d.M.y"))

(defn hakemus-pdf
  ([hakemus] (hakemus-pdf hakemus nil))
  ([hakemus esikatselu-message]
    (let [pvm (format-date (time/today))
          organisaatio (o/find-organisaatio (:organisaatioid hakemus))
          avustuskohteet (ak/find-avustuskohteet-by-hakemusid (:id hakemus))

          total-haettavaavustus (reduce + 0 (map :haettavaavustus avustuskohteet))
          total-omarahoitus (reduce + 0 (map :omarahoitus avustuskohteet))

          template (slurp (io/reader (io/resource (str "pdf-sisalto/templates/" (hakemus-template hakemus)))))]

      (pdf/muodosta-pdf
        {:otsikko {:teksti "Valtionavustushakemus" :paivays pvm :diaarinumero (:diaarinumero hakemus)}
         :teksti (xstr/interpolate template
                                   {:organisaatio-nimi (:nimi organisaatio)
                                    :organisaatiolaji-pl-gen (organisaatiolaji->plural-genetive (:lajitunnus organisaatio))
                                    :vireillepvm pvm
                                    :vuosi (:vuosi hakemus)
                                    :avustuskohteet (ak/avustuskohteet-section avustuskohteet)
                                    :haettuavustus total-haettavaavustus
                                    :omarahoitus total-omarahoitus
                                    :lahettaja (if esikatselu-message "<hakijan nimi, joka on lähettänyt hakemuksen>"
                                                                      (user/user-fullname user/*current-user*))})

         :footer (c/maybe-nil #(str "Liikennevirasto - esikatselu - " %) "Liikennevirasto" esikatselu-message)}))))

(defn find-hakemus-pdf [hakemusid]
  (let [hakemus (get-hakemus hakemusid)]
    (case (:hakemustilatunnus hakemus)
      "0" (hakemus-pdf hakemus (str "hakuaika ei ole alkanut"))
      "K" (hakemus-pdf hakemus (str "hakemus on keskeneräinen"))
      "T0" (hakemus-pdf hakemus (str "hakemus on täydennettävänä"))
      (if-let [asiakirja (:asiakirja (first (select-latest-hakemusasiakirja {:hakemusid hakemusid})))]
        (coerce/inputstream asiakirja)
        (ss/throw+ (str "Hakemuksen " hakemusid " asiakirjaa ei löydy hakemustilahistoriasta."))))))

;; *** Hakemustilan käsittely ***

(defn find-hakemuskausi [vuosi] (first (select-hakemuskausi vuosi)))

(defn change-hakemustila! [hakemus new-hakemustilatunnus expected-hakemustilatunnus operation]
  (dml/assert-update
    (update-hakemustila! {:hakemusid (:id hakemus)
                          :hakemustilatunnus new-hakemustilatunnus
                          :expectedhakemustilatunnus expected-hakemustilatunnus})

    {:http-response r/method-not-allowed
     :message (str "Hakemuksen (" (:id hakemus) ") " operation " ei ole sallittu tilassa: " (:hakemustilatunnus hakemus)
                   ". Hakemuksen " operation " on sallittu vain tilassa: " expected-hakemustilatunnus)
     :hakemusid (:id hakemus)
     :new-hakemustilatunnus new-hakemustilatunnus :expected-hakemustilatunnus expected-hakemustilatunnus})
  (email/send-hakemustapahtuma-message hakemus new-hakemustilatunnus))

(defn change-hakemustila+log!
  ([hakemus new-hakemustilatunnus expected-hakemustilatunnus operation]
    (change-hakemustila! hakemus new-hakemustilatunnus expected-hakemustilatunnus operation)

    ;; hakemustilan muutoshistoria
    (insert-hakemustila-event! {:hakemusid (:id hakemus)
                                :hakemustilatunnus new-hakemustilatunnus}))

  ([hakemus new-hakemustilatunnus expected-hakemustilatunnus operation asiakirja]
    (change-hakemustila! hakemus new-hakemustilatunnus expected-hakemustilatunnus operation)

    ;; hakemustilan muutoshistoria
    (insert-hakemustila-event+asiakirja! {:hakemusid (:id hakemus)
                                          :hakemustilatunnus new-hakemustilatunnus
                                          :asiakirja asiakirja})))

(defn laheta-hakemus! [hakemusid]
  (with-transaction
    (let [hakemus (get-hakemus hakemusid)
          hakemuskausi (find-hakemuskausi hakemus)
          hakemus-asiakirja (hakemus-pdf hakemus)
          organisaatio (o/find-organisaatio (:organisaatioid hakemus))

          liitteet (l/find-liitteet+sisalto hakemusid)]

      (change-hakemustila! hakemus "V" ["K"] "vireillelaitto")

      (if (= (:hakemustyyppitunnus hakemus) "AH0")
        (update-hakemus-set-diaarinumero!
          {:vuosi (:vuosi hakemus)
           :organisaatioid (:organisaatioid hakemus)
           :diaarinumero (asha/hakemus-vireille {:kausi (:diaarinumero hakemuskausi) :hakija (:nimi organisaatio)}
                                                hakemus-asiakirja liitteet)})

        (if-let [diaarinumero (:diaarinumero hakemus)]
          (let [kasittelija (user/find-user (or (:kasittelija hakemus)
                                                (:kasittelija (first (select-avustushakemus-kasittelija hakemus)))
                                                (:luontitunnus hakemus)))]
            (asha/maksatushakemus diaarinumero
                                  {:kasittelija (user/user-fullname kasittelija)
                                   :lahettaja (:nimi organisaatio)}
                                  hakemus-asiakirja liitteet))))

      (insert-hakemustila-event+asiakirja! {:hakemusid hakemusid
                                            :hakemustilatunnus "V"
                                            :asiakirja (hakemus-pdf (get-hakemus hakemusid))})))
  nil)

(defn tarkasta-hakemus! [hakemusid]
  (with-transaction
    (let [hakemus (get-hakemus hakemusid)]
      (change-hakemustila+log! hakemus "T" ["V" "TV"] "tarkastaminen")
      (save-hakemus-kasittelija! hakemusid (:tunnus user/*current-user*))
      (if-let [diaarinumero (:diaarinumero hakemus)]
        (asha/tarkastettu diaarinumero))))
  nil)

(defn maarapvm [loppupvm]
  (time/latest [loppupvm (time/plus (time/today) (time/days 14))]))

(defn add-taydennyspyynto! [hakemusid maarapaiva selite]
  (insert-taydennyspyynto! {:hakemusid hakemusid :maarapvm (coerce/localdate->sql-date maarapaiva) :selite selite}))

(defn taydennyspyynto!
  ([hakemusid] (taydennyspyynto! hakemusid nil))
  ([hakemusid selite]
  (with-transaction
    (let [hakemus (get-hakemus hakemusid)
          maarapvm (maarapvm (get-in hakemus [:hakuaika :loppupvm]))
          kasittelija user/*current-user*
          organisaatio (o/find-organisaatio (:organisaatioid hakemus))]

      (change-hakemustila+log! hakemus "T0" ["V" "TV"] "täydennyspyyntö")

      (add-taydennyspyynto! hakemusid maarapvm selite)
      (if-let [diaarinumero (:diaarinumero hakemus)]
        (asha/taydennyspyynto diaarinumero
                              {:maaraaika   (time/from-time-zone (timec/to-date-time maarapvm) (time/default-time-zone))
                               :kasittelija (user/user-fullname kasittelija)
                               :hakija      (:nimi organisaatio)}))))
  nil))

(defn laheta-taydennys! [hakemusid]
  (with-transaction
    (let [hakemus (get-hakemus+ hakemusid)
          hakemus-asiakirja (hakemus-pdf hakemus)
          organisaatio (o/find-organisaatio (:organisaatioid hakemus))
          kasittelija (user/find-user (or (:kasittelija hakemus) (:luontitunnus hakemus)))
          liitteet (l/find-liitteet+sisalto hakemusid)]

      (change-hakemustila+log! hakemus "TV" ["T0"] "täydentäminen" hakemus-asiakirja)

      (if-let [diaarinumero (:diaarinumero hakemus)]
        (asha/taydennys diaarinumero
                        {:kasittelija (user/user-fullname kasittelija)
                         :lahettaja (:nimi organisaatio)}
                        hakemus-asiakirja liitteet))))
  nil)