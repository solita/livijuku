(ns common.map
  (:require [clojure.string :as str]))

(defn remove-keys [m keys]
  (apply dissoc (concat [m] keys)))

(defn deep-merge
  "Deeply merges maps"
  [& vs]
  (if (every? map? vs)
    (apply merge-with deep-merge vs)
    (last vs)))

(defn keypath [key separator]
  "Split key to a keypath. Keypath item separator is separator regular expression."
  (map keyword (str/split (name key) separator)))

(defn add-prefix [prefix key separator]
  "Add prefix to a key. Returns a new key with specified prefix.
  If the prefix is nil then the original key is returned."
  (if prefix
    (-> key name (#(str/join separator [prefix %])) keyword)
    key))

(defn flat->tree [row separator]
  "Transforms a flat map object to a hierarchical object structure so that all keyvalues,
  which have the same prefix, are combined to a new map. This map contains all the
  keyvalues, which have the same prefix. The prefix is removed from the map keys.

  e.g. {:a_x 1, :a_y 2 :b 3} -> {a: {:x 1 :y 2} :b 3}"
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