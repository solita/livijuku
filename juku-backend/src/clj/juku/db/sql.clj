(ns juku.db.sql
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as str]
            [common.string :as strx]
            [common.core :as c]
            [slingshot.slingshot :as ss]))

(defn- parse-constraint-name [^String txt]
  (if-let [content (re-find #"\(JUKU.*\.(.*?)\)" txt)]
    (content 1)))

(defn- violated-constraint [^Throwable e]
  (if-let [message (.getMessage e)]
    (cond
      (strx/substring? "ORA-02291" message) (parse-constraint-name message)
      (strx/substring? "ORA-00001" message) (parse-constraint-name message)
      :else (c/maybe-nil violated-constraint nil (.getCause e)))))

(defn- default-error-message [sql params]
  (str "Failed to execute: " sql " - values: " params))

(defn with-db-exception-translation
  ([db-operation sql params constraint-violation-error error-parameters]
  (ss/try+
    (db-operation)
    (catch Exception e
      (c/if-let* [violated-constraint (violated-constraint e)
                  error (or (-> violated-constraint str/lower-case keyword constraint-violation-error) {})
                  message-template (or (:message error) (default-error-message sql params))]
        (ss/throw+ (merge {:sql sql :violated-constraint violated-constraint} error error-parameters)
                   (strx/interpolate message-template error-parameters))
        (ss/throw+ {:sql sql} (default-error-message sql params)))))))

(defn- db-do
  ([operation db sql params constraint-violation-error error-parameters]
    (with-db-exception-translation (fn [] (operation db sql params)) sql params constraint-violation-error error-parameters))

  ([operation db sql params] (db-do operation db sql params (constantly nil) {}) ))

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

(defn insert-with-id [db table row constraint-violation-error error-parameters]
  (db-do jdbc/db-do-prepared-return-keys db (insert-statement-with-id table row) (vals row)
         constraint-violation-error error-parameters))

(defn insert [db table row constraint-violation-error error-parameters]
  (db-do jdbc/db-do-prepared db (insert-statement-flatmap table row) (vals row)
         constraint-violation-error error-parameters))

;; update statements

(defn- assignment-expression [key]
  (str (name key) " = ?"))

(defn update-statement [table obj]
  (let [separator ", "
        set-clause  (str/join separator (map assignment-expression (keys obj)))]
       (str "update " table " set " set-clause)))

(defn update-where-id [db table obj id]
  (first (db-do jdbc/db-do-prepared db (str (update-statement table obj) " where id = ?") (concat (vals obj) [id]))))

(defmacro assert-update

  "Asserts that the update statement is successfully executed. Statement is successfull,
  if it has updated one or more rows (updatecount > 0).
  Error form is evaluated only if the statement is not successful (updatecount = 0).
  Error form should evaluate to error object, which is thrown in case of failure."

  [updatecount errorform]
    `(if (> ~updatecount 0) nil (let [e# ~errorform] (ss/throw+ e# (:message e#)))))
