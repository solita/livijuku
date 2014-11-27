(ns juku.db.hakemus
  (:require [yesql.core :as sql]
            [clojure.java.jdbc :as jdbc]
            [juku.db.osasto :as osasto]
            [juku.db.database :refer [db]]
            [juku.db.coerce :as coerce]
            [juku.db.sql :as dml]
            [schema.coerce :as scoerce]
            [juku.schema.hakemus :refer :all]
            [clj-time.core :as time]))


(sql/defqueries "hakemus.sql" {:connection db})

(defn hakemus-coercien-matcher [schema]
  (or
    (coerce/date->localdate-matcher schema)
    (coerce/number->int-matcher schema)))

(def coerce-hakemus (scoerce/coercer Hakemus hakemus-coercien-matcher))

(defn find-osaston-hakemukset [osastoid]
  (map (comp coerce-hakemus coerce/row->object)
    (select-osaston-hakemukset {:osastoid osastoid})))

(defn find-osaston-hakemukset-vuosittain [osastoid]
  (let [hakemukset (find-osaston-hakemukset osastoid)
        vuosittain (group-by :vuosi hakemukset)]
    (reduce (fn [result [key value]] (conj result {:vuosi key :hakemukset value}))
            '() vuosittain)))

(defn add-hakemus! [hakemus]
  (:id (dml/insert-with-id db "hakemus"
                           (-> hakemus
                               coerce/object->row
                               coerce/localdate->sql-date))))

(defn oletus-avustus-hakemus! [vuosi osastoid] {
     :vuosi vuosi :nro 1
     :osastoid osastoid
     :hakuaika {:alkupvm (time/local-date (- vuosi 1) 9 1)
                :loppupvm (time/local-date (- vuosi 1) 12 15)}})

(defn oletus-maksatus-hakemus1! [vuosi osastoid] {
     :vuosi vuosi :nro 2
     :osastoid osastoid
     :hakuaika {:alkupvm (time/local-date vuosi 7 1)
                :loppupvm (time/local-date vuosi 8 31)}})


(defn oletus-maksatus-hakemus2! [vuosi osastoid] {
       :vuosi vuosi :nro 3
       :osastoid osastoid
       :hakuaika {:alkupvm (time/local-date (+ vuosi 1) 1 1)
                  :loppupvm (time/local-date (+ vuosi 1) 1 31)}})

(defn avaa-hakemuskausi! [vuosi]
  (doseq [osasto (osasto/osastot)]
    (add-hakemus! (oletus-avustus-hakemus! vuosi (:id osasto)))
    (add-hakemus! (oletus-maksatus-hakemus1! vuosi (:id osasto)))
    (add-hakemus! (oletus-maksatus-hakemus2! vuosi (:id osasto)))))


