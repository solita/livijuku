(ns juku.service.organisaatio
  (:require [yesql.core :as sql]
            [clojure.java.jdbc :as jdbc]
            [juku.db.database :refer [db]]
            [juku.db.coerce :as coerce]
            [juku.db.sql :as dml]
            [common.collection :as c]
            [schema.coerce :as scoerce]))

(sql/defqueries "organisaatio.sql" {:connection db})

(defn organisaatiot [] (select-organisaatiot))

(defn hakija-organisaatiot [] (filter (c/predicate not= :lajitunnus "LV") (select-organisaatiot)))

(defn find-organisaatio [id] (c/find-first (c/eq :id id) (select-organisaatiot)))

(defn find-organisaatio-of [user] (find-organisaatio (:organisaatioid user)))
