(ns juku.service.hakemus
  (:require [yesql.core :as sql]
            [clojure.java.jdbc :as jdbc]
            [juku.service.organisaatio :as organisaatio]
            [juku.service.user :as user]
            [juku.db.database :refer [db]]
            [juku.db.coerce :as coerce]
            [juku.db.sql :as dml]
            [schema.coerce :as scoerce]
            [juku.schema.hakemus :refer :all]
            [clj-time.core :as time]
            [common.collection :as c]))


(sql/defqueries "hakemus.sql" {:connection db})

(defn hakemus-coercien-matcher [schema]
  (or
    (coerce/date->localdate-matcher schema)
    (coerce/number->int-matcher schema)))

(defn avustuskohde-coercien-matcher [schema]
  (or
    (coerce/number->int-matcher schema)))

(def coerce-hakemus (scoerce/coercer Hakemus hakemus-coercien-matcher))

(def coerce-avustuskohde (scoerce/coercer Avustuskohde avustuskohde-coercien-matcher))

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
  (hakemukset-group-by-hakemuskausi (find-organisaation-hakemukset
                                      (:organisaatioid user/*current-user*))))

(defn find-hakemukset-vuosittain []
  (hakemukset-group-by-hakemuskausi (find-all-hakemukset)))

(defn get-hakemus-by-id [hakemusid]
  (-> (select-hakemus {:hakemusid hakemusid})
    c/single-result
    coerce/row->object
    coerce-hakemus))

  #_
  (let [hakemus (c/single-result (select-hakemus {:hakemusid hakemusid}))
        avustuskohteet (select-avustuskohteet {:hakemusid hakemusid})]
    (assoc hakemus :avustuskohteet avustuskohteet))

(defn find-avustuskohteet-by-hakemusid [hakemusid]
  (map coerce-avustuskohde (select-avustuskohteet {:hakemusid hakemusid})))

(defn add-hakemus! [hakemus]
  (:id (dml/insert-with-id db "hakemus"
                           (-> hakemus
                               coerce/object->row
                               coerce/localdate->sql-date))))

(defn add-avustuskohde! [avustuskohde]
  (:id (dml/insert db "avustuskohde"
                           (-> avustuskohde
                               coerce/object->row
                               coerce/localdate->sql-date))))

(defn save-avustuskohde! [avustuskohde]
  (update-avustuskohde! avustuskohde))

(defn save-avustuskohteet! [avustuskohteet]
  (doseq [avustuskohde avustuskohteet]
    (if (= (save-avustuskohde! avustuskohde) 0)
      (add-avustuskohde! avustuskohde))))

(defn laheta-hakemus! [hakemusid]
  (update-hakemustila! {:hakemusid hakemusid :hakemustilatunnus "V"}))

(defn tarkasta-hakemus! [hakemusid]
  (update-hakemustila! {:hakemusid hakemusid :hakemustilatunnus "T"}))

(defn oletus-avustus-hakemus! [vuosi organisaatioid] {
     :vuosi vuosi :hakemustyyppitunnus "AH0"
     :organisaatioid organisaatioid
     :hakuaika {:alkupvm (time/local-date (- vuosi 1) 9 1)
                :loppupvm (time/local-date (- vuosi 1) 12 15)}})

(defn oletus-maksatus-hakemus1! [vuosi organisaatioid] {
     :vuosi vuosi :hakemustyyppitunnus "MH1"
     :organisaatioid organisaatioid
     :hakuaika {:alkupvm (time/local-date vuosi 7 1)
                :loppupvm (time/local-date vuosi 8 31)}})


(defn oletus-maksatus-hakemus2! [vuosi organisaatioid] {
       :vuosi vuosi :hakemustyyppitunnus "MH2"
       :organisaatioid organisaatioid
       :hakuaika {:alkupvm (time/local-date (+ vuosi 1) 1 1)
                  :loppupvm (time/local-date (+ vuosi 1) 1 31)}})

(defn avaa-hakemuskausi! [vuosi]
  (doseq [organisaatio (organisaatio/organisaatiot)]
    (add-hakemus! (oletus-avustus-hakemus! vuosi (:id organisaatio)))
    (add-hakemus! (oletus-maksatus-hakemus1! vuosi (:id organisaatio)))
    (add-hakemus! (oletus-maksatus-hakemus2! vuosi (:id organisaatio)))))


