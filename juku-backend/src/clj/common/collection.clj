(ns common.collection
  (:require [common.core :as c]
            [slingshot.slingshot :as ss])
  (:import (clojure.lang IPersistentCollection)))

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

(defn- join-
  [^IPersistentCollection target join-children ^IPersistentCollection source
   ^IPersistentCollection target-join-keys ^IPersistentCollection source-join-keys]
 (let [index (group-by (apply juxt source-join-keys) source)
       foreign-key-fn (apply juxt target-join-keys)]
   (map (fn [parent]
          (let [foreign-key (foreign-key-fn parent)
                matching-objects (get index foreign-key)]
            (join-children parent matching-objects)))
        target)))

(defn join
  "Each object (parent) in the target collection are joined with a subset (children) of the source collection.
  The children for the parent object are identified using join-keys.
  The child objects and the target object have the same join-key values e.g.
  (select-keys parent join-keys) = (select-keys child join-keys).
  The result of this join is determined using the join-children function."

  [^IPersistentCollection target join-children ^IPersistentCollection source join-condition]
  {:pre [(coll? target)
         (coll? source)
         (or (coll? join-condition) (map? join-condition))]}

  (if (map? join-condition)
    (join- target join-children source (keys join-condition) (vals join-condition))
    (join- target join-children source join-condition join-condition)))

(defn dissoc-join-keys
  "This defines an assoc-join transformation option which removes the join-keys from children."
  [collection keys] (map (fn [v] (apply dissoc v keys)) collection))

(defn no-transformation
  "The default assoc-join transformation option. The children are associated to the parent as is."
  [children _] (or children []))

(defn children [children _] children)

(defn assoc-join
  "Assoc-join is a join function (see join) where children are associated (assoc) to parents.
  Children can be tranformed before association using transform-children function: (children, join-keys) -> transformed children.
  The default transformation is (fn [children _] (if children children [])).
  The dissoc-join-keys transformation removes the join keys from the children."

  ([target new-key source eq-join-keys] (assoc-join target new-key source eq-join-keys no-transformation))
  ([target new-key source eq-join-keys transform-children]
    (join target
          (fn [parent child-set] (assoc parent new-key (transform-children child-set eq-join-keys)))
          source eq-join-keys)))

(defn assoc-join-if-not-nil

  ([target new-key source eq-join-keys] (assoc-join-if-not-nil target new-key source eq-join-keys no-transformation))
  ([target new-key source eq-join-keys transform-children]
    (join target
         (fn [parent child-set] (if child-set (assoc parent new-key (transform-children child-set eq-join-keys))
                                              parent))
         source eq-join-keys)))