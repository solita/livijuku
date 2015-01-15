(ns juku.service.paatos
  (:require [yesql.core :as sql]
            [clojure.java.jdbc :as jdbc]
            [juku.db.database :refer [db]]
            [juku.db.coerce :as coerce]
            [schema.coerce :as scoerce]
            [juku.schema.paatos :as s]
            [clojure.string :as str]
            [common.map :as m]))

(sql/defqueries "paatos.sql" {:connection db})

(def coerce-paatos (scoerce/coercer s/Paatos coerce/db-coercion-matcher))

(defn find-paatos [hakemusid]
  (first (map coerce-paatos (select-paatos {:hakemusid hakemusid}))))

(defn new-paatos! [paatos] (insert-paatos! paatos))

(defn save-paatos! [paatos]
  (let [updated (update-paatos! paatos)]
    (assert (< updated 2) "Tietokannan tiedot ovat virheelliset. Hakemuksella on kaksi avointa päätöstä.")
    (if (= updated 0) (new-paatos! paatos))
    nil))


#_(defn hyvaksy-paatos! [hakemusid] )
