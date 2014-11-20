(ns juku.db.conversion
  (:import java.sql.Date
           org.joda.time.LocalDate)
  (:require [clj-time.coerce :as time-coerce]
            [clj-time.core :as time]))

(defn convert-instances-of [c f m]
  (clojure.walk/postwalk #(if (instance? c %) (f %) %) m))

(defn joda-datetime->sql-timestamp [m]
  (convert-instances-of org.joda.time.DateTime
                        time-coerce/to-sql-time
                        m))

(defn sql-timestamp->joda-datetime [m]
  (convert-instances-of java.sql.Timestamp
                        time-coerce/from-sql-time
                        m))
