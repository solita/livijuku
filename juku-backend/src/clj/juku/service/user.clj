(ns juku.service.user
  (:require [yesql.core :as sql]
            [clojure.java.jdbc :as jdbc]
            [juku.db.database :refer [db]]
            [juku.db.coerce :as coerce]
            [schema.coerce :as scoerce]
            [juku.schema.user :as s]
            [clojure.string :as str]
            [common.map :as m]))

(sql/defqueries "user.sql" {:connection db})

(def ^:dynamic *current-user*)

(defn user-fullname [user]
  (str (:etunimi user) " " (:sukunimi user)))

(defn with-user*
  [user f]
  (binding [*current-user* user] (f)))

(defmacro with-user [user & body]
  `(with-user* ~user (fn [] ~@body)))

(def coerce-user (scoerce/coercer s/User coerce/db-coercion-matcher))

(defn find-user [tunnus]
  (coerce-user(m/dissoc-if-nil(first (select-user {:tunnus tunnus})) :nimi :etunimi :sukuni)))

(defn find-users-by-organization [organisaatioid]
  (map (comp coerce-user (fn [user] (m/dissoc-if-nil user :nimi :etunimi :sukunimi)))
       (select-users-where-organization {:organisaatioid organisaatioid})))