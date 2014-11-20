(ns juku.db.tietokanta
  (:require [clojure.java.jdbc])
  (:require [juku.db.jdbc_monkey_patch])
  (:import (java.sql PreparedStatement ResultSet)))

(def db-settings {:classname "oracle.jdbc.OracleDriver"  ; must be in classpath
                  :url "jdbc:oracle:thin:@localhost:1521:orcl"
                  :user "juku_app"
                  :password "juku"})

(defn pool
  [settings]
  (println (:url settings) (:user settings) (:password settings))
  (let [partitions 3
        cpds (doto (com.jolbox.bonecp.BoneCPDataSource.)
               (.setJdbcUrl (:url settings))
               (.setUsername (:user settings))
               (.setPassword (:password settings))
               (.setMinConnectionsPerPartition 5)
               (.setMaxConnectionsPerPartition 10)
               (.setPartitionCount partitions)
               (.setStatisticsEnabled true)
               ;; test connections every 25 mins (default is 240):
               (.setIdleConnectionTestPeriodInMinutes 25)
               ;; allow connections to be idle for 3 hours (default is 60 minutes):
               (.setIdleMaxAgeInMinutes (* 3 60))
               ;; consult the BoneCP documentation for your database:
               (.setConnectionTestStatement "/* ping *\\/ select 1 from dual"))]
    {:datasource cpds}))

(def db-connection (pool db-settings))

