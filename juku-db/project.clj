(defproject juku-db "1.8.0-SNAPSHOT"
	:min-lein-version "2.9.1"
	:repositories [["oracle" {:url "oam11g://maven.oracle.com"}]]

	:dependencies [[com.oracle.jdbc/ojdbc10 "19.3.0.0"]
                 [org.dbmaintain/dbmaintain "2.4"]
                 [org.clojure/clojure "1.10.1"]]
	
	:plugins [[lein-oracle-repository "0.2.0"] [lein-pprint "1.1.1"] [lein-ancient "0.6.15"] [lein-nvd "1.3.0"]]

	:profiles {:dev {:main juku.dbmaintain/dev-main}
             :test-data {:resource-paths ["test/sql"]}}

  :main juku.dbmaintain
  :uberjar-name "juku-db.jar"
  :resource-paths ["sql"]
  :aot :all
  :source-paths ["src/clj"]
)
