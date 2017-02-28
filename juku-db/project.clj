(defproject juku-db "1.5.2"
	:min-lein-version "2.5.1"
	:repositories [["oracle" {:url "oam11g://maven.oracle.com"}]]

	:dependencies [[com.oracle.jdbc/ojdbc7 "12.1.0.2"]
                 [org.dbmaintain/dbmaintain "2.4"]
                 [org.clojure/clojure "1.6.0"]]
	
	:plugins [[lein-oracle-repository "0.1.0-SNAPSHOT"] [lein-dbmaintain "0.1.3"] [lein-pprint "1.1.1"]]

	:dbmaintain {
			 :driver "oracle.jdbc.OracleDriver"
			 :url ~(str "jdbc:oracle:thin:@" (or (System/getenv "DB_URL") "localhost:1521:orcl"))
			 :user-name ~(or (System/getenv "DB_USER") "juku")
			 :password ~(or (System/getenv "DB_PASSWORD") "juku")
			 :schemas ~(or (System/getenv "DB_USER") "juku")
			 :scripts "sql"
			 :dialect "oracle"}

	:profiles {:dev {:plugins [[com.oracle.jdbc/ojdbc7 "12.1.0.2" :exclusions [com.oracle.jdbc/xmlparserv2]]]}
						 :test-data {:dbmaintain {:scripts "sql, test/sql"} :resource-paths ["test/sql"]}}

  :main juku.dbmaintain
  :uberjar-name "juku-db.jar"
  :resource-paths ["sql"]
  :aot :all
  :source-paths ["src/clj"]
)
