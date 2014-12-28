(ns common.collection)

(defn single-result [coll]
  (assert (= (count coll) 1) "A collection must contain one and only one item.")
  (first coll))

(defn starts-with [getter txt]
  (fn [obj] (.startsWith ^String (getter obj) txt)))