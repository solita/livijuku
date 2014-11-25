(ns juku.db.coerce
  (:require [clj-time.coerce :as time-coerce]
            [clj-time.core :as time]
            [common.string :as strx]
            [clojure.string :as str]
            [common.map :refer [remove-keys]]
            [schema.core :as sc]
            [schema.coerce :as scoerce]))


(defn timestamp->localdate-matcher [schema]
  (if (= schema org.joda.time.LocalDate)
    (fn [v]
      (if (instance? java.sql.Timestamp v)
        (time-coerce/to-local-date (time-coerce/from-sql-time v))
        v))))

(defn keypath [key]
  "Split key to a keypath. Keypath item separator is _."
  (map keyword (str/split (name key) #"_" )))

(defn add-prefix [prefix key]
  "Add prefix to a key. Returns a new key with specified prefix."
  (if prefix
    (-> key name (#(str/join "_" [prefix %])) keyword)
    key))

(defn row->object [row]
  "Transforms a database row to a more hierarchical object structure so that all keyvalues,
  which have the same prefix, are combined to a new map. This map contains all the
  keyvalues, which have the same prefix. The prefix is removed from the map keys.

  e.g. {:a_x 1, :a_y 2 :b 3} -> {a: {:x 1 :y 2} :b 3}"
  (reduce
    (fn [obj keyvalue]
      (let [key (first keyvalue)
            value (second keyvalue)]
        (assoc-in obj (keypath key) value))) {} row))

(defn object->row [obj & [prefix]]
  (reduce
    (fn [row keyvalue]
      (let [key (first keyvalue)
            value (second keyvalue)]
        (if (map? value)
          (merge row (object->row value (name key)))
          (assoc row (add-prefix prefix key) value)))) {} obj))