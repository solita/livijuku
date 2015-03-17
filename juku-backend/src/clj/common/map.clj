(ns common.map
  (:require [clojure.string :as str]))

(defn remove-keys [m keys]
  (apply dissoc (concat [m] keys)))

(defn update-in-if-exists [m key-path f & args]
  (if (get-in m key-path) (apply update-in m key-path f args) m))

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
  "Transforms a flat associative structure to a hierarchical structure so that all values
  are associated to a new map using a specific key path, which is generated from
  the current key of the value.

  Keypath is a sequence of keys and it is generated from splitting the current key
  using a given separator. Separator is a regular expression.

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

(defn dissoc-if-nil [m & keys]
  (reduce (fn [r key] (if (= (key r) nil) (dissoc r key) r)) m keys))