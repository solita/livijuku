(ns juku.db.osasto
  (:require [yesql.core :as sql]
            [clojure.java.jdbc :as jdbc]
            [juku.db.database :refer [db]]
            [juku.db.coerce :as coerce]
            [juku.db.sql :as dml]
            [schema.coerce :as scoerce]))

(sql/defqueries "osasto.sql" {:connection db})

(defn osastot [] (select-osastot))
