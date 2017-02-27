(ns common.map
  (:require [clojure.string :as str]
            [common.collection :as c]
            [common.core :as core])
  (:import (clojure.lang MapEntry)
           (java.util Properties)))

(defn remove-keys
  "Dissociate keys from a map. Removed keys are given in a sequence."
  [m keys]
  (apply dissoc m keys))

(defn update-in-if-exists
  "Updates a value in a nested associative structure (m) in a same way as update-in, if some value already exists in the keypath."
  [m key-path f & args]
  (if (get-in m key-path) (apply update-in m key-path f args) m))

(defn deep-merge-with
  "Deeply merges maps. If a key occurs in more than one map and
  any of the values is not a map then the mapping(s)
  from the latter (left-to-right) will be combined
  by calling (f val-in-result val-in-latter)."
  [f & vs]
  (if (every? map? vs)
    (apply merge-with (partial deep-merge-with f) vs)
    (f vs)))

(defn deep-merge
  "Deeply merges maps. Same as deep-merge-with where merge function is last."
  [& vs]
  (apply deep-merge-with last vs))

(defn mapentry? [value] (instance? MapEntry value))

(defn deep-reduce [f init coll]
  (reduce (fn [acc value]
            (if (or (map? value)
                    (and (coll? value) (not (mapentry? value))))
              (deep-reduce f acc value)
              (f acc value)))
          init coll))

(defn keypath
  "Split key to a keypath. Keypath item separator is separator regular expression."
  [key separator] (map keyword (str/split (name key) separator)))

(defn add-prefix
  "Add prefix to a key. Returns a new key with specified prefix.
  If the prefix is nil then the original key is returned."
  [prefix key separator]
  (if prefix
    (-> key name (#(str/join separator [prefix %])) keyword)
    key))

(defn flat->tree
  "Transforms a flat associative structure to a hierarchical structure so that all values
  are associated to a new map using a specific key path, which is generated from
  the current key of the value.

  Keypath is a sequence of keys and it is generated from splitting the current key
  using a given separator. Separator is a regular expression.

  e.g. {:a_x 1, :a_y 2 :b 3} -> {a: {:x 1 :y 2} :b 3}"

  [row separator]
  (reduce
    (fn [obj keyvalue]
      (let [key (first keyvalue)
            value (second keyvalue)]
        (assoc-in obj (keypath key separator) value))) {} row))

(defn tree->flat [obj separator & [prefix]]
  (reduce
    (fn [row keyvalue]
      (let [key (add-prefix prefix (first keyvalue) separator)
            value (second keyvalue)]
        (if (map? value)
          (merge row (tree->flat value separator (name key)))
          (assoc row key value)))) {} obj))

(defn keys-to-keywords [m]
  (into {} (for [[k v] m]
             [(-> k name
                    .toLowerCase
                    (str/replace "_" "-")
                    keyword)
              v])))

(defn dissoc-if
  "Remove keys from an associative structure based on values. It is optionally possible to limit removing only to certain keys."
  ([m predicate & keys] (reduce (fn [r key] (if (predicate (key r)) (dissoc r key) r)) m keys))
  ([m predicate] (into {} (remove #(predicate (second %)) m))))

(defn dissoc-if-nil
  "Remove null values from a map. It is optionally possible to limit filtering only to certain keys."
  ([m & keys] (apply dissoc-if m nil? keys))
  ([m] (dissoc-if m nil?)))

(defn map-values [f m] (into {} (map (fn [[k, v]] [k (f v)]) m)))

(defn every-value? [predicate map] (every? predicate (vals map)))

(defn update-vals
  ([object key-update-function-map] (reduce #(update %1 (first %2) (second %2)) object key-update-function-map))
  ([object keys update-function] (reduce #(update %1 %2 update-function) object keys)))

(defn zip-object [keys values]
  (into {} (map vector keys values)))

(defn ->properties [object]
  (reduce (fn [^Properties result property]
            (.setProperty result (str (first property)) (str (second property)))
            result)
          (Properties.) object))