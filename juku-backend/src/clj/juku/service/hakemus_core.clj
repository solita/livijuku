(ns juku.service.hakemus-core
  (:require [juku.db.yesql-patch :as sql]
            [juku.service.user :as user]
            [juku.db.database :refer [db with-transaction]]
            [juku.db.coerce :as coerce]
            [juku.db.sql :as dml]
            [juku.service.email :as email]
            [schema.coerce :as scoerce]
            [clojure.java.io :as io]
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

(defn find-kayttajan-hakemukset []
  (find-organisaation-hakemukset (:organisaatioid user/*current-user*)))

(defn- get-any-hakemus [hakemusid select]
  (-> (select {:hakemusid hakemusid})
      (coll/single-result-required! {:type ::hakemus-not-found
                                     :hakemusid hakemusid
                                     :message (str "Hakemusta " hakemusid " ei ole olemassa.")})
      coerce/row->object))

(defn is-hakemus-owner?* [hakemus]
  (= (:organisaatioid user/*current-user*) (:organisaatioid hakemus)))

(defn has-privilege-to-view-hakemus-content* [hakemus]
  (or (user/has-privilege* :view-kaikki-hakemukset)
      (and (is-hakemus-owner?* hakemus) (user/has-privilege* :view-oma-hakemus))
      (and (user/has-privilege* :view-kaikki-lahetetyt-hakemukset)
           (not (#{"0" "K" "T0"} (:hakemustilatunnus hakemus))))))

(defn get-hakemus+ [hakemusid]
  (let [hakemus (get-any-hakemus hakemusid select-hakemus+)]
    (coerce-hakemus+
      (assoc
        (if (= (:hakemustilatunnus hakemus) "T0")
          (assoc hakemus :taydennyspyynto (first (select-latest-taydennyspyynto {:hakemusid hakemusid})))
          hakemus)

        :contentvisible (has-privilege-to-view-hakemus-content* hakemus)
        :other-hakemukset (select-other-hakemukset {:hakemusid hakemusid})))))

(defn get-hakemus [hakemusid] (coerce-hakemus (get-any-hakemus hakemusid select-hakemus)))

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

(defn find-hakemuskausi [vuosi] (first (select-hakemuskausi vuosi)))

(defn ^String format-date [^LocalDate date] (.toString ^LocalDate date "d.M.y"))

(def organisaatiolaji->plural-genetive
  {"KS1" "suurten kaupunkiseutujen",
   "KS2" "keskisuurten kaupunkiseutujen",
   "ELY" "ELY-keskusten"})

;; *** Hakemustilan käsittely ***

(defn change-hakemustila! [hakemus new-hakemustilatunnus expected-hakemustilatunnus operation asiakirja]
  (dml/assert-update
    (update-hakemustila! {:hakemusid (:id hakemus)
                          :hakemustilatunnus new-hakemustilatunnus
                          :expectedhakemustilatunnus expected-hakemustilatunnus})

    {:http-response r/method-not-allowed
     :message (str "Hakemuksen (" (:id hakemus) ") " operation " ei ole sallittu tilassa: " (:hakemustilatunnus hakemus)
                   ". Hakemuksen " operation " on sallittu vain tilassa: " expected-hakemustilatunnus)
     :hakemusid (:id hakemus)
     :new-hakemustilatunnus new-hakemustilatunnus :expected-hakemustilatunnus expected-hakemustilatunnus})
  (email/send-hakemustapahtuma-message hakemus new-hakemustilatunnus asiakirja))

(defn change-hakemustila+log!
  ([hakemus new-hakemustilatunnus expected-hakemustilatunnus operation]
   (change-hakemustila! hakemus new-hakemustilatunnus expected-hakemustilatunnus operation nil)

    ;; hakemustilan muutoshistoria
   (insert-hakemustila-event! {:hakemusid (:id hakemus)
                               :hakemustilatunnus new-hakemustilatunnus}))

  ([hakemus new-hakemustilatunnus expected-hakemustilatunnus operation asiakirja]
   (let [asiakirja-bytes (c/slurp-bytes asiakirja)]
     (change-hakemustila! hakemus new-hakemustilatunnus expected-hakemustilatunnus operation asiakirja-bytes)

     ;; hakemustilan muutoshistoria
     (insert-hakemustila-event+asiakirja! {:hakemusid (:id hakemus)
                                           :hakemustilatunnus new-hakemustilatunnus
                                           :asiakirja (io/input-stream asiakirja-bytes)}))))

