(defproject juku-backend "1.2.3"
            :description "Liikennevirasto - Joukkoliikenteen rahoitus-, kustannus- ja suoritetietojen keräys- ja seurantajärjestelmä"
            :url "https://extranet.liikennevirasto.fi/juku/"
            :min-lein-version "2.4.3"
            :repositories [["solita" {:url "http://mvn.solita.fi/repository/solita"}]]

            :dependencies [[org.clojure/clojure "1.7.0"]
                           [org.clojure/java.jdbc "0.4.2"]
                           [slingshot "0.12.2"]
                           [clj-time "0.11.0"]
                           [com.google.guava/guava "18.0"]
                           [org.apache.pdfbox/pdfbox "1.8.10"]

                           [clj-http "2.0.0"]
                           [com.draines/postal "1.11.4"]

                           [environ "1.0.1"] ;; Library for managing environment variables in Clojure.

                           ;; *** web application ***
                           [http-kit "2.1.19"] ;; http server
                           [ring "1.4.0"]
                           [ring/ring-defaults "0.1.5"]

                           [metosin/compojure-api "0.23.1"]
                           [metosin/ring-swagger-ui "2.1.5-M2"]

                           ;; *** datababse ***
                           [oracle/ojdbc7 "12.1.0.2"]
                           [yesql "0.5.1"]
                           [com.zaxxer/HikariCP "2.4.1"]

                           ;; *** logging libraries ***
                           [org.clojure/tools.logging "0.3.1"]
                           [ch.qos.logback/logback-classic "1.1.3"]]

            :plugins [[test2junit "1.1.0"]
                      [lein-ring "0.8.12"]
                      ;; Library for managing environment settings from a number of different sources
                      [lein-environ "1.0.0"]]

            :ring {:handler      juku.handler/app
                   :uberwar-name "juku.war"}

            :profiles {:dev     {:dependencies [[javax.servlet/servlet-api "2.5"]
                                                [ring-mock "0.1.5"]
                                                ; Midje provides a migration path from clojure.test to a more flexible, readable, abstract, and gracious style of testing.
                                                [midje "1.7.0"]
                                                ;;[midje-junit-formatter "0.1.0-SNAPSHOT"]
                                                [clj-http-fake "1.0.1"]
                                                [metosin/scjsv "0.2.0"]]

                                 :plugins      [; Run multiple leiningen tasks in parallel.
                                                [lein-pdo "0.1.1"]
                                                [lein-midje "3.1.3"]

                                                [lein-ancient "0.6.7"]
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
