(ns common.collection
  (:require [slingshot.slingshot :as ss]
            [ring.util.http-response :as http]))

(defmacro not-found!
  [type parameters msg] `(ss/throw+ (merge {:type ~type :http-response http/not-found} ~parameters) ~msg))

(defn assert-not-empty! [coll]
  (if (empty? coll) (not-found! ::empty {} "The collection is empty")))

(defn single-result-required [coll]
  (assert (<= (count coll) 1) "The collection contains more than one item.")
  (assert-not-empty! coll)
  (first coll))

(defn starts-with [getter txt]
  (fn [obj] (.startsWith ^String (getter obj) txt)))