(ns juku.service.hakemus
  (:require [juku.db.yesql-patch :as sql]
            [juku.service.user :as user]
            [juku.db.database :refer [db with-transaction]]
            [juku.db.coerce :as coerce]
            [juku.db.sql :as dml]
            [juku.service.organisaatio :as o]
            [juku.service.asiahallinta :as asha]
            [juku.service.avustuskohde :as ak]
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

(defn get-hakemus-by-id [hakemusid]
  (-> (select-hakemus {:hakemusid hakemusid})
      (coll/single-result-required ::hakemus-not-found {:hakemusid hakemusid} (str "Hakemusta " hakemusid " ei ole olemassa."))
      coerce/row->object
      coerce-hakemus+))

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
  (dml/update-where-id db "hakemus" hakemus hakemusid))

(defn save-hakemus-selite! [hakemusid selite]
  (update-hakemus-by-id {:selite selite} hakemusid))

(defn save-hakemus-suunniteltuavustus! [hakemusid suunniteltuavustus]
  (update-hakemus-by-id {:suunniteltuavustus suunniteltuavustus} hakemusid))

(defn save-hakemus-kasittelija! [hakemusid kasittelija]
  (update-hakemus-by-id {:kasittelija kasittelija} hakemusid))

(defn get-hakemus-by-id! [hakemusid]
  (let [hakemus (get-hakemus-by-id hakemusid)
        diaarinumero (:diaarinumero hakemus)]
    (if (and (user/has-privilege* "kasittely-hakemus")
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

(defn hakemus-pdf [hakemusid]
  (let [vireillepvm-txt (.toString ^LocalDate (time/today) "d.M.y")
        hakemus (get-hakemus-by-id hakemusid)
        organisaatio (o/find-organisaatio (:organisaatioid hakemus))
        avustuskohteet (ak/find-avustuskohteet-by-hakemusid hakemusid)

        total-haettavaavustus (reduce + 0 (map :haettavaavustus avustuskohteet))
        total-omarahoitus (reduce + 0 (map :omarahoitus avustuskohteet))

        template (slurp (io/reader (io/resource "pdf-sisalto/templates/hakemus.txt")))]

    (pdf/muodosta-pdf
      {:otsikko {:teksti "Valtionavustushakemus" :paivays vireillepvm-txt :diaarinumero (:diaarinumero hakemus)}
       :teksti (xstr/interpolate template
                                 {:organisaatio-nimi (:nimi organisaatio)
                                  :organisaatiolaji-pl-gen (organisaatiolaji->plural-genetive (:lajitunnus organisaatio))
                                  :vireillepvm vireillepvm-txt
                                  :vuosi (:vuosi hakemus)
                                  :avustuskohteet (ak/avustuskohteet-section avustuskohteet)
                                  :haettuavustus total-haettavaavustus
                                  :omarahoitus total-omarahoitus})
       :footer "Liikennevirasto"})))

;; *** Hakemustilan käsittely ***

(defn find-hakemuskausi [vuosi] (first (select-hakemuskausi vuosi)))

(defn change-hakemustila! [hakemusid new-hakemustilatunnus expected-hakemustilatunnus operation]
  (dml/assert-update
    (update-hakemustila! {:hakemusid hakemusid
                          :hakemustilatunnus new-hakemustilatunnus
                          :expectedhakemustilatunnus expected-hakemustilatunnus})

    (let [hakemus (get-hakemus-by-id hakemusid)]
      {:http-response r/method-not-allowed
       :message (str "Hakemuksen (" hakemusid ") " operation " ei ole sallittu tilassa: " (:hakemustilatunnus hakemus)
                     ". Hakemuksen " operation " on sallittu vain tilassa: " expected-hakemustilatunnus)
       :hakemusid hakemusid
       :new-hakemustilatunnus new-hakemustilatunnus :expected-hakemustilatunnus expected-hakemustilatunnus})))

(defn change-hakemustila+log!
  ([hakemusid new-hakemustilatunnus expected-hakemustilatunnus operation]
    (change-hakemustila! hakemusid new-hakemustilatunnus expected-hakemustilatunnus operation)

    ;; hakemustilan muutoshistoria
    (insert-hakemustila-event! {:hakemusid hakemusid
                                :hakemustilatunnus new-hakemustilatunnus}))

  ([hakemusid new-hakemustilatunnus expected-hakemustilatunnus operation asiakirja]
    (change-hakemustila! hakemusid new-hakemustilatunnus expected-hakemustilatunnus operation)

    ;; hakemustilan muutoshistoria
    (insert-hakemustila-event+asiakirja! {:hakemusid hakemusid
                                          :hakemustilatunnus new-hakemustilatunnus
                                          :asiakirja asiakirja})))

(defn laheta-hakemus! [hakemusid]
  (with-transaction
    (let [hakemus (get-hakemus-by-id hakemusid)
          hakemuskausi (find-hakemuskausi hakemus)
          hakemus-asiakirja (hakemus-pdf hakemusid)
          organisaatio (o/find-organisaatio (:organisaatioid hakemus))

          liitteet (l/find-liitteet+sisalto hakemusid)]

      (change-hakemustila! hakemusid "V" ["K"] "vireillelaitto")

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
                                            :asiakirja (hakemus-pdf hakemusid)})))
  nil)

(defn tarkasta-hakemus! [hakemusid]
  (with-transaction
    (let [hakemus (get-hakemus-by-id hakemusid)]
      (change-hakemustila+log! hakemusid "T" ["V" "TV"] "tarkastaminen")
      (if-let [diaarinumero (:diaarinumero hakemus)]
        (asha/tarkastettu diaarinumero))))
  nil)

(defn maarapvm [loppupvm]
  (time/latest [loppupvm (time/plus (time/today) (time/days 14))]))

(defn add-taydennyspyynto! [hakemusid maarapaiva]
  (insert-taydennyspyynto! {:hakemusid hakemusid :maarapvm (coerce/localdate->sql-date maarapaiva)}))

(defn taydennyspyynto! [hakemusid]
  (with-transaction
    (let [hakemus (get-hakemus-by-id hakemusid)
          maarapvm (maarapvm (get-in hakemus [:hakuaika :loppupvm]))
          kasittelija user/*current-user*
          organisaatio (o/find-organisaatio (:organisaatioid hakemus))]

      (change-hakemustila+log! hakemusid "T0" ["V" "TV"] "täydennyspyyntö")

      (add-taydennyspyynto! hakemusid maarapvm)
      (if-let [diaarinumero (:diaarinumero hakemus)]
        (asha/taydennyspyynto diaarinumero
                              {:maaraaika   (time/from-time-zone (timec/to-date-time maarapvm) (time/default-time-zone))
                               :kasittelija (user/user-fullname kasittelija)
                               :hakija      (:nimi organisaatio)}))))
  nil)

(defn laheta-taydennys! [hakemusid]
  (with-transaction
    (let [hakemus (get-hakemus-by-id hakemusid)
          hakemus-asiakirja (hakemus-pdf hakemusid)
          organisaatio (o/find-organisaatio (:organisaatioid hakemus))
          kasittelija (user/find-user (or (:kasittelija hakemus) (:luontitunnus hakemus)))
          liitteet (l/find-liitteet+sisalto hakemusid)]

      (change-hakemustila+log! hakemusid "TV" ["T0"] "täydentäminen" hakemus-asiakirja)

      (if-let [diaarinumero (:diaarinumero hakemus)]
        (asha/taydennys diaarinumero
                        {:kasittelija (user/user-fullname kasittelija)
                         :lahettaja (:nimi organisaatio)}
                        hakemus-asiakirja liitteet))))
  nil)