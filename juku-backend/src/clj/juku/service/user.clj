(ns juku.service.user
  (:require [yesql.core :as sql]
            [clojure.java.jdbc :as jdbc]
            [juku.db.database :refer [db]]
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

(defn find-user [tunnus]
  (first (select-user {:tunnus tunnus})))