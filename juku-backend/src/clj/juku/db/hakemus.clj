(ns juku.db.hakemus
  (:require [yesql.core :as sql]
            [clojure.java.jdbc :as jdbc]
            [juku.db.database :refer [db]]
            [juku.db.coerce :as coerce]
            [juku.db.sql :as dml]
            [schema.coerce :as scoerce]
            [juku.schema.hakemus :refer :all]))


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

#_
(defn add-hakemus! [hakemus]
  (jdbc/with-db-transaction [tx db]
        (:id (insert-hakemus<! (-> hakemus
          coerce/object->row
          coerce/localdate->sql-date) {:connection tx}))))


