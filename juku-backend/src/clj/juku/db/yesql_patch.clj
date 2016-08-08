(ns juku.db.yesql-patch
  (:require [yesql.generate :as generate]
            [yesql.core :as yesql]
            [juku.db.sql :as sql]
            [juku.db.database :refer [db]]
            [common.map :as m]
            [clojure.string :as str]
            [clojure.java.jdbc :as jdbc]))

(defn query-handler
  [db sql-and-params
   {:keys [row-fn result-set-fn identifiers as-arrays?]
    :or   {identifiers   str/lower-case
           row-fn        identity
           result-set-fn doall
           as-arrays?    false}
    :as   call-options}]
  (jdbc/query db sql-and-params
              :identifiers identifiers
              :row-fn row-fn
              :result-set-fn result-set-fn
              :as-arrays? as-arrays?))

(alter-var-root #'generate/query-handler (constantly query-handler))

(defn set-connection [args]
  (case (count args)
    0 [{}, {:connection db}]
    1 (concat args [{:connection db}])
    args))

(defonce original-generate-query-fn generate/generate-query-fn)

(defn generate-query-fn [this options]
  (let [original (original-generate-query-fn this options)
        sql (:yesql.generate/source (meta original))
        wrapper (fn [& args] (sql/with-db-exception-translation
                               (partial apply original (set-connection args)) sql
                               (first args)
                               (or (:constraint-errors options) {})
                               (m/remove-keys (first args) (:dissoc-error-params options))))]
    (with-meta wrapper (meta original))))

(alter-var-root #'generate/generate-query-fn (constantly generate-query-fn))

(defn defqueries
  ([filename] (yesql/defqueries filename {}))
  ([filename options] (yesql/defqueries filename options)))


