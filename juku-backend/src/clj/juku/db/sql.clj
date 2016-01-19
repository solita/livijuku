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
      (strx/substring? "ORA-02291" message)
        {:violated-constraint (parse-constraint-name message) :type ::foreign-key-constraint}
      (strx/substring? "ORA-00001" message)
        {:violated-constraint (parse-constraint-name message) :type ::unique-constraint}
      :else (c/maybe-nil violated-constraint nil (.getCause e)))))

(defn- default-error-message [sql params]
  (str "Failed to execute: " sql " - values: " (str/join ", " params)))

(defn with-db-exception-translation
  ([db-operation sql params constraint-violation-error error-parameters]
  (ss/try+
    (db-operation)
    (catch Exception e
      (c/if-let* [constraint (violated-constraint e)
                  error (or (-> constraint :violated-constraint str/lower-case keyword constraint-violation-error) {})
                  message-template (or (:message error) (default-error-message sql params))]
        (ss/throw+ (merge {:sql sql} constraint error error-parameters)
                   (strx/interpolate message-template error-parameters))
        (ss/throw+ {:sql sql} (default-error-message sql params)))))))

(defn- db-do
  ([operation db sql params constraint-violation-error error-parameters]
    (with-db-exception-translation (fn [] (operation db sql params)) sql params constraint-violation-error error-parameters))

  ([operation db sql params] (db-do operation db sql params (constantly nil) {}) ))

(defn isValidDatabaseIdentifier [identifier]
  (c/not-nil? (re-matches #"\p{Lower}+[\p{Lower}_]*" (str/lower-case identifier))))

;; insert statements

(defn- insert-statement [table columns values]
  {:pre [(isValidDatabaseIdentifier table)
         (every? isValidDatabaseIdentifier columns)]}
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
  {:pre [(isValidDatabaseIdentifier (str (name key)))]}
  (str (name key) " = ?"))

(defn update-statement [table obj]
  {:pre [(isValidDatabaseIdentifier table)]}
  (let [separator ", "
        set-clause  (str/join separator (map assignment-expression (keys obj)))]
       (str "update " table " set " set-clause)))

(defn update-where-id [db table obj id]
  (first (db-do jdbc/db-do-prepared db (str (update-statement table obj) " where id = ?") (concat (vals obj) [id]))))

(defn- where-clause [where]
  (str/join " and " (map assignment-expression (keys where))))

(defn- update-statement+where [table obj where]
  (str (update-statement table obj) " where " (where-clause where)))

(defn update-where! [db table obj where]
  (first (db-do jdbc/db-do-prepared db
                (update-statement+where table obj where)
                (concat (vals obj) (vals where)))))

(defn update-batch-where! [db table obj-list where-list]
  (let [sql (update-statement+where table (first obj-list) (first where-list))
        param-groups (map concat (map vals obj-list) (map vals where-list))]
    (with-db-exception-translation
      (fn [] (apply jdbc/db-do-prepared db sql param-groups))
      sql {} (constantly nil) {})))

(defmacro assert-update

  "Asserts that the update statement is successfully executed. Statement is successfull,
  if it has updated one or more rows (updatecount > 0).
  Error form is evaluated only if the statement is not successful (updatecount = 0).
  Error form should evaluate to an error object, which is thrown in case of a failure."

  [updatecount errorform]
    `(if (> ~updatecount 0) nil (let [e# ~errorform] (ss/throw+ e# (:message e#)))))

(defn insert-ignore-unique-constraint-error [insert-operation & args]
  (ss/try+
    (apply insert-operation args)
    (catch [:type ::unique-constraint] {})))

(defn upsert
  "Update or insert, if not exists a row to the database.
   DB operations must use db-exception-translation defined in this namespace.
   Update operation must return the count of rows updated."

  [update-operation insert-operation & args]
  (case (apply update-operation args)
    0 (ss/try+
        (apply insert-operation args)
        (catch [:type ::unique-constraint] {}
          (apply update-operation args)))
    1 nil
    (ss/throw+ {:message "Unexpected system error - too many rows updated"})))
