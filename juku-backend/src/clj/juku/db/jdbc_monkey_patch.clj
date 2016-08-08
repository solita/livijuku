(ns juku.db.jdbc_monkey_patch
  (:require [clojure.java.jdbc]
            [juku.db.oracle-metrics :as metrics]
            [juku.user :refer [*current-user-id*]])
  (:import (java.sql Connection)))

(defonce original-prepare-statement clojure.java.jdbc/prepare-statement)

;; a patch for clojure.java.jdbc

(defn prepare-statement
  "This is a small patch to prepare statement in clojure.java.jdbc.
  This will set oracle end to end metrics to the connection and
  if return keys are not explicitly defined (:return-keys = true) then id-column is set as return key.
  In oracle return keys must be explicitly defined as column names."

  ([^Connection con ^String sql] (prepare-statement con sql {}))
  ([^Connection con ^String sql options]
  (do
    (metrics/set-end-to-end-metrics con *current-user-id*
                                    (:name metrics/*module*)
                                    (:action metrics/*module*))

    (if (true? (:return-keys options))
      (original-prepare-statement con sql (assoc options :return-keys ["id"]))
      (original-prepare-statement con sql options)))))

(alter-var-root #'clojure.java.jdbc/prepare-statement (constantly prepare-statement))