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
  (:import (org.joda.time LocalDate )
           (org.joda.time DateTime )))

(defn date->localdate-matcher [schema]
  (if (= schema LocalDate)
    (fn [v]
      (if (instance? java.util.Date v)
        (LocalDate. (.getTime v))
        v))))

(defn date->datetime-matcher [schema]
  (if (= schema DateTime)
    (fn [v]
      (if (instance? java.util.Date v)
        (DateTime. (.getTime v))
        v))))

(defn number->int-matcher [schema]
  (if (= schema sc/Int)
    (fn [v]
      (if (instance? Number v) (int v) v))))

(defn number->boolean-matcher [schema]
  (if (= schema sc/Bool)
    (fn [v]
      (if (instance? Number v) (not= v 0) v))))

;; TODO check if this works for strings longer than 4000 bytes
(defn clob->str-matcher [schema]
  (if (= schema sc/Str)
    (fn [v]
      (if (instance? java.sql.Clob v)
        (let [length (.length v)] (.getSubString v 1 length)) v))))

(defn- convert-instances-of [c f m]
  (clojure.walk/postwalk #(if (instance? c %) (f %) %) m))

(defn localdate->sql-date [m]
  (convert-instances-of org.joda.time.LocalDate time-coerce/to-sql-date m))

(defn row->object [row]
  "Transforms a database row to a more hierarchical object structure so that all keyvalues,
  which have the same prefix, are combined to a new map. This map contains all the
  keyvalues, which have the same prefix. The prefix is removed from the map keys.

  e.g. {:a_x 1, :a_y 2 :b 3} -> {a: {:x 1 :y 2} :b 3}"
  (m/flat->tree row #"_"))

(defn object->row [obj]
  (m/tree->flat obj "_"))
