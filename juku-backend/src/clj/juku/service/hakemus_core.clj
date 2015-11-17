(ns juku.service.hakemus-core
  (:require [juku.db.yesql-patch :as sql]
            [juku.service.user :as user]
            [juku.db.database :refer [db with-transaction]]
            [juku.db.coerce :as coerce]
            [juku.db.sql :as dml]
            [juku.service.email :as email]
            [slingshot.slingshot :as ss]
            [schema.coerce :as scoerce]
            [clojure.java.io :as io]
            [juku.schema.hakemus :refer :all]
            [ring.util.http-response :as r]
            [common.collection :as coll]
            [common.core :as c]
            [common.map :as map])
  (:import (org.joda.time LocalDate)))

; *** Hakemukseen liittyvät kyselyt ***
(sql/defqueries "hakemus.sql")

; *** Hakemus skeemaan liittyvät konversiot tietokannan tietotyypeistä ***
(def coerce-hakemus (coerce/coercer Hakemus))
(def coerce-hakemus+kasittely (coerce/coercer Hakemus+Kasittely))
(def coerce-hakemus+ (coerce/coercer Hakemus+))
(def coerce-hakemus-suunnitelma (coerce/coercer HakemusSuunnitelma))

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
  (map (comp coerce-hakemus+kasittely coerce/row->object)
       (select-all-hakemukset)))

(defn find-kayttajan-hakemukset []
  (find-organisaation-hakemukset (:organisaatioid user/*current-user*)))

(defn get-any-hakemus [hakemusid select]
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

(defn is-hakemus-keskenerainen?
  "Tässä keskeneräinen-sana on laajemmassa merkityksessä ja tarkoittaa yleisesti kaikkia hakemuksen tiloja,
   joissa hakemus on hakijalla työstettävänä. Nämä tilat ovat: keskeneräinen (K) ja täydennettävänä (T0)."
  [hakemus] (#{"K" "T0"} (:hakemustilatunnus hakemus)))

(defn throw! [response msg] (ss/throw+ {:http-response response :message msg} msg))

(defn assert-view-hakemus-content-allowed*! [hakemus]
  (when-not (has-privilege-to-view-hakemus-content* hakemus)
    (throw! r/forbidden
            (str "Käyttäjällä " (:tunnus user/*current-user*)
            " ei ole oikeutta nähdä hakemuksen: " (:id hakemus) " sisältöä. "
            "Käyttäjä ei ole hakemuksen omistaja ja käyttäjällä ei ole oikeutta nähdä keskeneräisiä hakemuksia."))))

(defn assert-edit-hakemus-content-allowed*!
  "Hakemusta voi muokata vain jos kaikki seuraavat ehdot täyttyvät:
   - käyttäjällä on hakemuksen muokkausoikeus
   - käyttäjä on hakemuksen omistaja
   - hakemus on keskeneräinen"

  [hakemus]
  (when-not (user/has-privilege* :modify-oma-hakemus)
    (throw! r/forbidden
            (str "Käyttäjällä " (:tunnus user/*current-user*)
                 " ei ole oikeutta muokata hakemuksen: " (:id hakemus) " sisältöä.")))

  (when-not (is-hakemus-owner?* hakemus)
    (throw! r/forbidden
            (str "Käyttäjä " (:tunnus user/*current-user*) " ei ole hakemuksen: " (:id hakemus) " omistaja.")))

  (when (= (:hakemustilatunnus hakemus) "0")
    (throw! r/conflict (str "Hakemusta " (:id hakemus) " ei voi muokata, koska hakuaika ei ole alkanut.")))

  (when-not (is-hakemus-keskenerainen? hakemus)
    (throw! r/conflict (str "Hakemusta " (:id hakemus) " ei voi muokata, koska se ei ole enää keskeneräinen. "
                            "Hakemus on lähetetty käsiteltäväksi."))))

(defn get-hakemus+ [hakemusid]
  (let [hakemus (map/dissoc-if (get-any-hakemus hakemusid select-hakemus+) (partial map/every-value? nil?) :ely)]
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
                               coerce/object->row)
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

(defn change-hakemustila! [hakemus new-hakemustilatunnus expected-hakemustilatunnus operation]
  (dml/assert-update
    (update-hakemustila! {:hakemusid (:id hakemus)
                          :hakemustilatunnus new-hakemustilatunnus
                          :expectedhakemustilatunnus expected-hakemustilatunnus})

    {:http-response r/conflict
     :message (str "Hakemuksen (" (:id hakemus) ") " operation " ei ole sallittu tilassa: " (:hakemustilatunnus hakemus)
                   ". Hakemuksen " operation " on sallittu vain tilassa: " expected-hakemustilatunnus)
     :hakemusid (:id hakemus)
     :new-hakemustilatunnus new-hakemustilatunnus :expected-hakemustilatunnus expected-hakemustilatunnus}))

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

