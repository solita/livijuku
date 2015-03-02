(ns juku.dbmaintain
  (:gen-class)
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str])
  (:import
    (org.dbmaintain MainFactory)
    (java.util Properties)))

(def ohje
  (clojure.string/join \newline
     ["Usage: java -jar juku-db.jar [command]"
      "Parameters: DB_URL, DB_USER and DB_PASSWORD can be defined as environment variables. Otherwise common default values are used. See juku-backend/juku-example-properties."
      "Database url (DB_URL) should not contain the prefix: 'jdbc:oracle:thin:@'."
      "Supported commands are: "
      "- check-db"
      "- clear-db"
      "- update-db"
      "- mark-up-to-date"]))

(defn resource-url []
      (let [sql-url (.getResource (.getClass MainFactory) "/project.clj")]
           (if (= (.getProtocol sql-url) "jar")
             (let [path (.getPath sql-url)
                   index (.indexOf path "!")]
               (.substring path 5 index))
             (.getPath (.getParentFile (io/as-file sql-url))))))

(defn make-properties []
  (doto (Properties.)
    (.load (.getResourceAsStream MainFactory "/dbmaintain-default.properties"))
    (.put "dbMaintainer.script.locations" (resource-url))
    (.put "dbMaintainer.autoCreateDbMaintainScriptsTable" "true")
    (.put "dbMaintainer.script.encoding" "UTF-8")
    (.put "database.driverClassName" "oracle.jdbc.driver.OracleDriver")
    (.put "database.url" (str "jdbc:oracle:thin:@" (or (System/getenv "DB_URL") "localhost:1521:orcl")))
    (.put "database.userName" (or (System/getenv "DB_USER") "juku"))
    (.put "database.password" (or (System/getenv "DB_PASSWORD") "juku"))
    (.put "database.schemaNames" (or (System/getenv "DB_USER") "juku"))
    (.put "database.dialect" "oracle")))

(defn ^MainFactory get-mainfactory
      [properties]
      (MainFactory. properties))

(defn clean-db
    [properties]
    (let [factory (get-mainfactory properties)]
         (.. factory createDBCleaner cleanDatabase)))

(defn clear-db
    [properties]
    (let [factory (get-mainfactory properties)]
         (.. factory createDBClearer clearDatabase)))

(defn disable-constraints
    [properties]
    (let [factory (get-mainfactory properties)]
         (.. factory createConstraintsDisabler disableConstraints)))

(defn mark-up-to-date
    [properties]
    (let [factory (get-mainfactory properties)]
         (.. factory createDbMaintainer markDatabaseAsUpToDate)))

(defn update-db
    [properties]
    (let [factory (get-mainfactory properties)]
         (.updateDatabase (.createDbMaintainer factory) false)))

(defn check-db
    [properties]
    (let [factory (get-mainfactory properties)]
      (.updateDatabase (.createDbMaintainer factory) true)))

(defn update-sequences
    [properties]
    (let [factory (get-mainfactory properties)]
         (.. factory createSequenceUpdater updateSequences)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (let [command (str/trim (or (first args) "<empty string>"))
        properties (make-properties)]
    (case command
      "check-db" (check-db properties)
      "clear-db" (clear-db properties)
      "update-db" (update-db properties)
      "mark-up-to-date" (mark-up-to-date properties)
      (do
        (println "Unsupported command:" command)
        (println ohje)))))



