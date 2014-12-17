(ns juku.db.sql
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as str]))

(defn- db-do [operation db sql params]
  (try
    (operation db sql params)
    (catch Exception e
      (throw (RuntimeException.
               (str "Failed to execute: " sql " - values: " params) e)))))

;; insert statements

(defn insert-statement [table columns values]
  (let [separator ", "]
    (str "insert into " table " (" (str/join separator columns)
         ") values (" (str/join separator values) ")")))

(defn insert-statement-with-id [table row]
  (let [columns (concat ["id"] (map name (keys row)))
        values (concat [(str table "_seq.nextval")] (repeat (count row) "?"))]
    (insert-statement table columns values)))

(defn insert-statement-flatmap [table row]
  (let [columns (map name (keys row))
        values (repeat (count row) "?")]
    (insert-statement table columns values)))

(defn insert-with-id [db table row]
  (db-do jdbc/db-do-prepared-return-keys db (insert-statement-with-id table row) (vals row)))

(defn insert [db table row]
  (db-do jdbc/db-do-prepared db (insert-statement-flatmap table row) (vals row)))

;; update statements

(defn update-statement [table obj]
  (let [separator ", "
        set-clause  (str/join separator (map (fn [key] (str (name key) " = ?")) (keys obj)))]
       (str "update " table " set " set-clause)))

(defn update-where-id [db table obj id]
  (first (db-do jdbc/db-do-prepared db (str (update-statement table obj) " where id = ?") (concat (vals obj) [id]))))