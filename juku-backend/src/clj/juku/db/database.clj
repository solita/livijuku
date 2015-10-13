(ns juku.db.database
  (:require [clojure.java.jdbc :as jdbc]
            [juku.db.jdbc_monkey_patch]
            [clj-time.coerce :as time-coerce]
            [clj-time.core :as time]
            [slingshot.slingshot :as ss]
            [clojure.tools.logging :as log]
            [juku.settings :refer [settings]]
            [juku.db.coerce :as coerce])
  (:import [com.zaxxer.hikari HikariConfig HikariDataSource]
           (java.io InputStream)
           (java.sql PreparedStatement Array Date)
           (clojure.lang IPersistentCollection)
           (oracle.jdbc OracleConnection)
           (org.joda.time LocalDate)
           (org.joda.time DateTime)
           (java.util Calendar TimeZone)))


(def db-settings (:db settings))

(defn data-source [settings]
  (log/info "Starting database connection pool: " (:url settings) (:user settings) "****")
  (HikariDataSource. (doto (HikariConfig.)
                       (.setMaximumPoolSize 10)
                       (.setDriverClassName "oracle.jdbc.OracleDriver")
                       (.setJdbcUrl (:url settings))
                       (.setUsername (:user settings))
                       (.setPassword (:password settings))
                       (.setAutoCommit false))))

(defonce ^:dynamic db {:datasource (data-source db-settings)})

(defn setup-shutdown-hook! [f]
  (.addShutdownHook (Runtime/getRuntime) (Thread. f)))

(defn shutdown [] (.shutdown (:datasource db)))

(setup-shutdown-hook! shutdown)

(defmacro with-transaction [& body] `(jdbc/with-db-transaction [tx# db] (binding [db tx#] ~@body)))

(defmacro with-transaction*
  ([tx-options & body] `(jdbc/with-db-transaction [tx# db ~@tx-options] (binding [db tx#] ~@body))))


; *** clojure.java.jdbc protocol extensions ***

(def ^:private ^Calendar utc-calendar_template
  (doto (Calendar/getInstance (TimeZone/getTimeZone "UTC")) .clear))

(defn ^Calendar utc-calendar [] (.clone utc-calendar_template))

(extend-protocol jdbc/ISQLParameter
  InputStream
  (set-parameter [^InputStream v ^PreparedStatement s  i]
    (.setBlob s i v))
  IPersistentCollection
  (set-parameter [^IPersistentCollection v ^PreparedStatement s  i]
    (if-let [db-type (::db-type (meta v))]
      (.setArray s i (.createARRAY (.unwrap (.getConnection s) OracleConnection)
                                   db-type
                                   (to-array v)))
      (ss/throw+ "Collection type sql parameter value object must contain db-type definition in the metadata.")))
  DateTime
  (set-parameter [^DateTime v ^PreparedStatement s  i]
    (.setTimestamp s i (time-coerce/to-sql-time v)))
  LocalDate
  (set-parameter [^LocalDate v ^PreparedStatement s  i]
    (let [^Calendar cal (utc-calendar)]
      (.set cal (time/year v) (- (time/month v) 1) (time/day v))
      (.setDate s i (Date. (.getTimeInMillis cal)) cal)))
  #_(set-parameter [^LocalDate v ^PreparedStatement s  i]
                   (.setDate s i (time-coerce/to-sql-date v))))

(extend-protocol jdbc/IResultSetReadColumn
  Array
  (result-set-read-column [x _ _] (vec (.getArray x))))