(ns juku.service.organisaatio
  (:require [yesql.core :as sql]
            [clojure.java.jdbc :as jdbc]
            [juku.db.database :refer [db]]
            [juku.db.coerce :as coerce]
            [juku.db.sql :as dml]
            [schema.coerce :as scoerce]))

(sql/defqueries "organisaatio.sql" {:connection db})

(defn organisaatiot [] (select-organisaatiot))

(defn hakija-organisaatiot [] (filter (fn [org] (not= (:lajitunnus org) "LV")) (select-organisaatiot)))
