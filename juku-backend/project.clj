(defproject juku-backend "1.7.1"
            :description "Liikennevirasto - Joukkoliikenteen rahoitus-, kustannus- ja suoritetietojen keräys- ja seurantajärjestelmä"
            :url "https://extranet.liikennevirasto.fi/juku/"
            :min-lein-version "2.9.1"
            :repositories [["oracle" {:url "oam11g://maven.oracle.com"}]]

            :dependencies [[org.clojure/clojure "1.10.1"]
                           [slingshot "0.12.2"]
                           [clj-time "0.15.1"]
                           [com.google.guava/guava "23.0"]
                           [org.apache.pdfbox/pdfbox "2.0.15"]
                           [clojure-csv/clojure-csv "2.0.2"]
                           [clj-pdf "2.3.5"]

                           [clj-http "3.10.0"]
                           [com.draines/postal "2.0.3"]

                           [environ "1.0.3"] ;; Library for managing environment variables in Clojure.

                           ;; *** web application ***
                           [http-kit "2.3.0"] ;; http server
                           [ring "1.7.1"]
                           [ring/ring-defaults "0.3.2"]

                           [metosin/compojure-api "1.1.12"]
                           [metosin/ring-swagger-ui "3.20.1"]

                           ;; *** datababse ***
                           [com.oracle.jdbc/ojdbc7 "12.1.0.2"]
                           [org.clojure/java.jdbc "0.7.9"]
                           [yesql "0.5.3"]
                           [honeysql "0.9.4"]
                           [com.zaxxer/HikariCP "3.3.1"]

                           ;; *** logging libraries ***
                           [org.clojure/tools.logging "0.4.1"]
                           [ch.qos.logback/logback-classic "1.2.3"]]

            :managed-dependencies [[org.flatland/ordered "1.5.7"]]

            :plugins [[lein-oracle-repository "0.2.0"]
                      [test2junit "1.1.0"]
                      [lein-ring "0.8.12"]]

            :ring {:handler      juku.handler/app
                   :uberwar-name "juku.war"}

            :profiles {:dev     {:dependencies [[javax.servlet/servlet-api "2.5"]
                                                [ring-mock "0.1.5"]
                                                ; Midje provides a migration path from clojure.test to a more flexible, readable, abstract, and gracious style of testing.
                                                [midje "1.9.8"]
                                                ;;[midje-junit-formatter "0.1.0-SNAPSHOT"]
                                                [clj-http-fake "1.0.3"]
                                                [metosin/scjsv "0.5.0"]]

                                 :plugins      [; Run multiple leiningen tasks in parallel.
                                                [lein-pdo "0.1.1"]
                                                [lein-midje "3.1.3"]

                                                [lein-ancient "0.6.15"]
                                                [lein-kibit "0.1.2"]
                                                [jonase/eastwood "0.2.1"]]

                                 :jvm-opts ["-Duser.timezone=EET"]

                                 ; What to do in the case of version issues - tehdään näin (ignore) koska muuten tulee valitusta leiniltä
                                 ; (ja koska viisaammatkin ihmiset on näin uskaltaneet tehdä)
                                 :pedantic?    false
                                 :source-paths ["dev-src/clj"]

                                 :env {:is-dev true}}
                       :uberjar {:main           juku.main
                                 :aot            [juku.main]
                                 :resource-paths ["swagger-ui"]
                                 :env {:is-dev false}}
                       :ci      {:resource-paths ["resources/ci"]}}

            :aot [juku.main]
            :main juku.main
            :source-paths ["src/clj"]
            :resource-paths ["resources" "src/sql"]
            :test-paths ["test/clj"]
            :uberjar-name "juku.jar")
