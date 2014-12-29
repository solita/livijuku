(ns juku.db.coerce
  (:require [clj-time.coerce :as time-coerce]
            [clj-time.core :as time]
            [common.string :as strx]
            [common.map :as m]
            [clojure.string :as str]
            [common.map :refer [remove-keys]]
            [schema.core :as sc]
            [schema.coerce :as scoerce]
            )
  (:import (org.joda.time LocalDate)
           (org.joda.time DateTime)
           (java.util Date)))

(defn date->localdate [^Date v] (LocalDate. (.getTime v)))

(defn date->datetime [^Date v] (DateTime. (.getTime v)))

(defn number->int [^Number v] (int v))

(defn number->boolean [^Number v] (not (== v 0)))

;; TODO check if this works for strings longer than 4000 bytes
(defn clob->string [^java.sql.Clob v]
  (let [length (.length v)] (.getSubString v 1 length)))

(defn- create-match-fn [conversion]
  (let [var-conversion (if (var? conversion) conversion (resolve conversion))
        type (-> var-conversion meta :arglists first first meta :tag resolve)]
    (fn [v] (if (instance? type v) (var-conversion v) v))))

(defn create-matcher [conversion-map]
  (into {} (for [[schema conversion] conversion-map]
             [schema (create-match-fn conversion)])))

(def db-coercion-matcher (create-matcher
    {LocalDate   'date->localdate
     DateTime    'date->datetime
     sc/Int      'number->int
     sc/Bool     'number->boolean
     sc/Str      'clob->string}))

(defn- convert-instances-of [c f m]
  (clojure.walk/postwalk #(if (instance? c %) (f %) %) m))

(defn localdate->sql-date [m]
  (convert-instances-of LocalDate time-coerce/to-sql-date m))

(defn row->object [row]
  "Transforms a database row to a more hierarchical object structure so that all keyvalues,
  which have the same prefix, are combined to a new map. This map contains all the
  keyvalues, which have the same prefix. The prefix is removed from the map keys.

  e.g. {:a_x 1, :a_y 2 :b 3} -> {a: {:x 1 :y 2} :b 3}"
  (m/flat->tree row #"_"))

(defn object->row [obj]
  (m/tree->flat obj "_"))
