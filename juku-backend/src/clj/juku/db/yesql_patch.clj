(ns juku.db.yesql-patch
  (:require [yesql.generate :as generate]
            [yesql.core :as yesql]
            [juku.db.sql :as sql]
            [juku.db.database :refer [db]]
            [common.map :as m])
  (:import [yesql.types Query]))

(defn set-connection [args]
  (case (count args)
    0 [{}, {:connection db}]
    1 (concat args [{:connection db}])
    args))

(extend-type Query
  generate/FunctionGenerator
  (generate-fn [this options]
    (let [original (generate/generate-query-fn this options)
          sql (:yesql.generate/source (meta original))
          wrapper (fn [& args] (sql/with-db-exception-translation
                                 (partial apply original (set-connection args)) sql
                                 (first args)
                                 (or (:constraint-errors options) {})
                                 (m/remove-keys (first args) (:dissoc-error-params options))))]
      (with-meta wrapper (meta original)))))

(defn defqueries
  ([filename] (yesql/defqueries filename {}))
  ([filename options] (yesql/defqueries filename options)))

