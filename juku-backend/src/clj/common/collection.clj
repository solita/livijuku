(ns common.collection
  (:require [clojure.set :as set]
            [common.map :as m]
            [slingshot.slingshot :as ss]
            [ring.util.http-response :as http]))

(defmacro not-found!
  [type parameters msg] `(ss/throw+ (merge {:type ~type :http-response http/not-found} ~parameters) ~msg))

(defn assert-not-empty! [coll type parameters message]
  (if (empty? coll) (not-found! type parameters message)))

(defn single-result-required [coll type parameters message]
  (assert (<= (count coll) 1) "The collection contains more than one item.")
  (assert-not-empty! coll type parameters message)
  (first coll))

(defn starts-with [getter txt]
  (fn [obj] (.startsWith ^String (getter obj) txt)))

(defn eq [getter value]
  (fn [obj] (= (getter obj) value)))

(defn assoc-left-join [new-key target-rel join-rel & eq-join-keys]
  (let [index (set/index join-rel eq-join-keys)]
    (map (fn [parent]
           (let [foreign-key (select-keys parent eq-join-keys)
                 value (or (get index foreign-key) #{})]
             (assoc parent new-key value)))
         target-rel)))