(ns juku.db.yesql-patch
  (:require [yesql.generate :as generate]
            [juku.db.sql :as sql])
  (:import [yesql.types Query]))


(extend-type Query
  generate/FunctionGenerator
  (generate-fn [this options]
    (let [original (generate/generate-query-fn this options)
          sql (:yesql.generate/source (meta original))
          wrapper (fn [& args] (sql/with-db-exception-translation (partial apply original args) sql (first args) (or (:constraint-errors options) {}) (first args)))]
      (with-meta wrapper (meta original)))))


