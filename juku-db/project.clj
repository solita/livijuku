(defproject juku-db "1.6.0"
	:min-lein-version "2.5.1"
	:repositories [["oracle" {:url "oam11g://maven.oracle.com"}]]

	:dependencies [[com.oracle.jdbc/ojdbc7 "12.1.0.2"]
                 [org.dbmaintain/dbmaintain "2.4"]
                 [org.clojure/clojure "1.8.0"]]
	
	:plugins [[lein-oracle-repository "0.1.0"] [lein-pprint "1.1.1"]]

	:profiles {:dev {:main juku.dbmaintain/dev-main}
             :test-data {:resource-paths ["test/sql"]}}

  :main juku.dbmaintain
  :uberjar-name "juku-db.jar"
  :resource-paths ["sql"]
  :aot :all
  :source-paths ["src/clj"]
)
