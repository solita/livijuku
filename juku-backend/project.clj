(defproject juku-backend "1.7.0-SNAPSHOT"
            :description "Liikennevirasto - Joukkoliikenteen rahoitus-, kustannus- ja suoritetietojen keräys- ja seurantajärjestelmä"
            :url "https://extranet.liikennevirasto.fi/juku/"
            :min-lein-version "2.4.3"
            :repositories [["oracle" {:url "oam11g://maven.oracle.com"}]]

            :dependencies [[org.clojure/clojure "1.7.0"]
                           [slingshot "0.12.2"]
                           [clj-time "0.12.0"]
                           [com.google.guava/guava "19.0"]
                           [org.apache.pdfbox/pdfbox "1.8.11"]
                           [clojure-csv/clojure-csv "2.0.2"]
                           [clj-pdf "2.2.1"]

                           [clj-http "2.0.1"]
                           [com.draines/postal "2.0.0"]

                           [environ "1.0.3"] ;; Library for managing environment variables in Clojure.

                           ;; *** web application ***
                           [http-kit "2.1.19"] ;; http server
                           [ring "1.5.0"]
                           [ring/ring-defaults "0.2.1"]

                           [metosin/compojure-api "1.1.3"]
                           [metosin/ring-swagger-ui "2.1.5-M2"]

                           ;; *** datababse ***
                           [com.oracle.jdbc/ojdbc7 "12.1.0.2"]
                           [org.clojure/java.jdbc "0.6.1"]
                           [yesql "0.5.3"]
                           [honeysql "0.7.0"]
                           [com.zaxxer/HikariCP "2.6.0"]

                           ;; *** logging libraries ***
                           [org.clojure/tools.logging "0.3.1"]
                           [ch.qos.logback/logback-classic "1.1.7"]]

            :plugins [[lein-oracle-repository "0.1.0"]
                      [test2junit "1.1.0"]
                      [lein-ring "0.8.12"]]

            :ring {:handler      juku.handler/app
                   :uberwar-name "juku.war"}

            :profiles {:dev     {:dependencies [[javax.servlet/servlet-api "2.5"]
                                                [ring-mock "0.1.5"]
                                                ; Midje provides a migration path from clojure.test to a more flexible, readable, abstract, and gracious style of testing.
                                                [midje "1.8.3"]
                                                ;;[midje-junit-formatter "0.1.0-SNAPSHOT"]
                                                [clj-http-fake "1.0.2"]
                                                [metosin/scjsv "0.2.0"]]

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
