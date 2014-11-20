(ns juku.db.jdbc_monkey_patch
  (:require [clojure.java.jdbc])
  (:import (java.sql PreparedStatement ResultSet)))

(def ^{:private true
       :doc "Map friendly :concurrency values to ResultSet constants."}
     result-set-concurrency
  {:read-only ResultSet/CONCUR_READ_ONLY
   :updatable ResultSet/CONCUR_UPDATABLE})

(def ^{:private true
       :doc "Map friendly :cursors values to ResultSet constants."}
     result-set-holdability
  {:hold ResultSet/HOLD_CURSORS_OVER_COMMIT
   :close ResultSet/CLOSE_CURSORS_AT_COMMIT})

(def ^{:private true
       :doc "Map friendly :type values to ResultSet constants."}
     result-set-type
  {:forward-only ResultSet/TYPE_FORWARD_ONLY
   :scroll-insensitive ResultSet/TYPE_SCROLL_INSENSITIVE
   :scroll-sensitive ResultSet/TYPE_SCROLL_SENSITIVE})

;; monkey patch for clojure.java.jdbc
(defn prepare-statement
  "Create a prepared statement from a connection, a SQL string and an
   optional list of parameters:
     :return-keys true | false - default false
     :result-type :forward-only | :scroll-insensitive | :scroll-sensitive
     :concurrency :read-only | :updatable
     :cursors
     :fetch-size n
     :max-rows n
     :timeout n"
  [^java.sql.Connection con ^String sql &
   {:keys [return-keys result-type concurrency cursors
           fetch-size max-rows timeout]}]
  (let [^PreparedStatement
        stmt (cond return-keys
                   (.prepareStatement con sql (into-array ["id"]))

                   (and result-type concurrency)
                   (if cursors
                     (.prepareStatement con sql
                                        (get result-set-type result-type result-type)
                                        (get result-set-concurrency concurrency concurrency)
                                        (get result-set-holdability cursors cursors))
                     (.prepareStatement con sql
                                        (get result-set-type result-type result-type)
                                        (get result-set-concurrency concurrency concurrency)))

                   :else
                   (.prepareStatement con sql))]
    (when fetch-size (.setFetchSize stmt fetch-size))
    (when max-rows (.setMaxRows stmt max-rows))
    (when timeout (.setQueryTimeout stmt timeout))
    stmt))

(alter-var-root #'clojure.java.jdbc/prepare-statement (constantly prepare-statement))