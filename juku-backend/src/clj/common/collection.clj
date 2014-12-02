(ns common.collection)

  (defn single-result [coll]
    (assert (= (count coll) 1) "A collection must contain one and only one item.")
    (first coll))