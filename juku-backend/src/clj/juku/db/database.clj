(ns juku.db.database
  (:require [clojure.java.jdbc :as jdbc]
            [juku.db.jdbc_monkey_patch]
            [clojure.tools.logging :as log]
            [juku.settings :refer [settings]])
  (:import [com.zaxxer.hikari HikariConfig HikariDataSource]
           (java.io InputStream)
           (java.sql PreparedStatement)))


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

(extend-protocol jdbc/ISQLParameter
  InputStream
  (set-parameter [^InputStream v ^PreparedStatement s  i]
    (.setBlob s i v)))

(defmacro with-transaction [& body] `(jdbc/with-db-transaction [tx# db] (binding [db tx#] ~@body)))

(defmacro with-transaction*
  ([tx-options & body] `(jdbc/with-db-transaction [tx# db ~@tx-options] (binding [db tx#] ~@body))))