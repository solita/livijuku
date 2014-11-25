(ns juku.db.sql
  (:import (java.sql SQLException))
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as str]))

(defn insert-statement [table columns values]
  (let [separator ", "]
    (str "insert into " table " (" (str/join separator columns)
         ") values (" (str/join separator values) ")")))

(defn insert-statement-with-id [table row]
  (let [columns (concat ["id"] (map name (keys row)))
        values (concat [(str table "_seq.nextval")] (repeat (count row) "?"))]
    (insert-statement table columns values)))

(defn insert-with-id [db table row]
  (let [sql (insert-statement-with-id table row)]
    (try
      (jdbc/db-do-prepared-return-keys db sql (vals row))
      (catch Exception e
        (throw (RuntimeException.
          (str "insert: " sql " - values: " (vals row)) e))))))