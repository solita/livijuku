(ns common.collection
  (:require [clojure.set :as set]
            [slingshot.slingshot :as ss]))

(defn not-found! [error] (ss/throw+ (merge error {:type ::not-found}) (:message error)))

(defn assert-not-empty! [coll error]
  (if (empty? coll) (not-found! error)))

(defn single-result!
  "Returns a single result item from the given collection.
   If the collection is empty or nil, returns nil.
	 Throws the given error object if more than 1 item is found."
  ([coll] (single-result! coll {}))
  ([coll error] (if (> (count coll) 1)
                  (ss/throw+ (merge error {:type ::ambiguous-result :size (count coll)})
                             (or (:message error) "The collection contains more than one item.")))
  (first coll)))

(defn single-result-required!
  "Returns a single result item from the given collection.
   If the collection is empty or nil, throws a required error object.
	 Throws an ambiguous error object if more than 1 item is found."

  ([coll required-error] (single-result-required! coll required-error {}))
  ([coll required-error ambiguous-error]
  (if-let [result (single-result! coll ambiguous-error)]
    result
    (not-found! required-error))))

(defn nil-if-empty [col] (if (empty? col) nil col))

(defn find-first [predicate collection]
  (first (filter predicate collection)))

(defn predicate [operator getter value]
  (fn [obj] (operator (getter obj) value)))

(defn or* [& fs]
  (fn [obj] (find-first (fn [x] (if x x)) (map (fn [f] (f obj)) fs))))

(defn starts-with [getter txt]
  (fn [obj] (.startsWith ^String (getter obj) txt)))

(defn eq [getter value]
  (predicate = getter value))

(defn join
  "Join function joins a subset of objects from the source collection to an object in the target collection.
  The source subset (joinset) for a target object is identified using the join-keys.
  Joined source objects and the target object have same join-key values.
  The result of this join is determined using the join-fn function."

  [target join-fn source eq-join-keys]
  (let [index (set/index source eq-join-keys)]
    (map (fn [parent]
           (let [foreign-key (select-keys parent eq-join-keys)
                 matching-objects (get index foreign-key)]
             (join-fn parent matching-objects)))
         target)))

(defn assoc-left-join [target-rel new-key join-rel & eq-join-keys]
  (join target-rel (fn [parent value] (assoc parent new-key (or value #{}))) join-rel eq-join-keys))

(defn- dissoc-keys [collection keys] (set (map (fn [v] (apply dissoc v keys)) collection)))

(defn assoc-left-join*
  ([target-rel new-key join-rel default-value eq-join-keys]
    (join target-rel
          (fn [parent child-set] (assoc parent new-key (dissoc-keys (or child-set default-value) eq-join-keys)))
          join-rel eq-join-keys))

  ([target-rel new-key join-rel eq-join-keys]
    (join target-rel
          (fn [parent child-set] (if child-set (assoc parent new-key (dissoc-keys child-set eq-join-keys)) parent))
          join-rel eq-join-keys)))