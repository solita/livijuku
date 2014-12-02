(ns juku.db.database
  (:require [clojure.java.jdbc]
            [juku.db.jdbc_monkey_patch]
            [juku.settings :refer [settings]])
  (:import [com.zaxxer.hikari HikariConfig HikariDataSource]))


(def db-settings (:db settings))

(defn data-source [settings]
  (println (:url settings) (:user settings) (:password settings))
  (HikariDataSource. (doto (HikariConfig.)
                       (.setMaximumPoolSize 10)
                       (.setDriverClassName "oracle.jdbc.OracleDriver")
                       (.setJdbcUrl (:url settings))
                       (.setUsername (:user settings))
                       (.setPassword (:password settings)))))
  #_
  (doto (com.jolbox.bonecp.BoneCPDataSource.)
               (.setJdbcUrl (:url settings))
               (.setUsername (:user settings))
               (.setPassword (:password settings))
               (.setMinConnectionsPerPartition 5)
               (.setMaxConnectionsPerPartition 10)
               (.setPartitionCount 3)
               (.setStatisticsEnabled true)
               ;; test connections every 25 mins (default is 240):
               (.setIdleConnectionTestPeriodInMinutes 25)
               ;; allow connections to be idle for 3 hours (default is 60 minutes):
               (.setIdleMaxAgeInMinutes (* 3 60))
               ;; consult the BoneCP documentation for your database:
               (.setConnectionTestStatement "select /* ping */ 1 from dual"))

(def db {:datasource (data-source db-settings)})

(defn setup-shutdown-hook! [f]
  (.addShutdownHook (Runtime/getRuntime) (Thread. f)))

(defn shutdown [] (.shutdown (:datasource db)))

(setup-shutdown-hook! shutdown)

