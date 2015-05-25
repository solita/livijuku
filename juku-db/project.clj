(defproject juku-db "1.0.0"
  :min-lein-version "2.5.1"
	:dependencies [[oracle/ojdbc6 "11.2.0.3.0"]
                 [org.dbmaintain/dbmaintain "2.4"]
                 [org.clojure/clojure "1.6.0"]]
	
	:plugins [[lein-dbmaintain "0.1.3"] [oracle/ojdbc6 "11.2.0.3.0"] [lein-pprint "1.1.1"]]

	:dbmaintain {
			 :driver "oracle.jdbc.OracleDriver"
			 :url ~(str "jdbc:oracle:thin:@" (or (System/getenv "DB_URL") "localhost:1521:orcl"))
			 :user-name ~(or (System/getenv "DB_USER") "juku")
			 :password ~(or (System/getenv "DB_PASSWORD") "juku")
			 :schemas ~(or (System/getenv "DB_USER") "juku")
			 :scripts "sql"
			 :dialect "oracle"}

	:repositories [["solita" {:url "http://mvn.solita.fi/archiva/repository/solita/" :snapshots true}]]

	:profiles {:test-data {:dbmaintain {:scripts "sql, test/sql"} :resource-paths ["test/sql"]}}

  :main juku.dbmaintain
  :uberjar-name "juku-db.jar"
  :resource-paths ["sql"]
  :aot :all
  :source-paths ["src/clj"]
)
