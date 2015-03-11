(defproject juku-backend "0.1.0-SNAPSHOT"
            :description "Liikennevirasto - Joukkoliikenteen rahoitus-, kustannus- ja suoritetietojen keräys- ja seurantajärjestelmä"
            :url "http://example.com/FIXME"
            :min-lein-version "2.4.3"

            :dependencies [[org.clojure/clojure "1.6.0"]
                           [org.clojure/java.jdbc "0.3.6"]
                           [slingshot "0.12.1"]
                           [clj-time "0.8.0"]
                           [com.google.guava/guava "18.0"]

                           [environ "1.0.0"] ;; Library for managing environment variables in Clojure.

                           ;; *** web application ***
                           [http-kit "2.1.10"] ;; http server
                           [ring "1.3.1"]
                           [ring/ring-defaults "0.1.2"]

                           [metosin/compojure-api "0.17.0"]
                           [metosin/ring-swagger-ui "2.0.24"]

                           ;; *** datababse ***
                           [org.clojars.zentrope/ojdbc "11.2.0.3.0"]
                           [yesql "0.5.0-beta2"]
                           [com.zaxxer/HikariCP-java6 "2.2.5"]

                           ;; *** logging libraries ***
                           [org.clojure/tools.logging "0.3.1"]
                           [ch.qos.logback/logback-classic "1.1.2"]

                           ;; Timbre brings functional, Clojure-y goodness to all your logging needs.
                           [com.taoensso/timbre "3.3.1"]]

            :plugins [[test2junit "1.1.0"]
                      [lein-ring "0.8.12"]
                      ;; Library for managing environment settings from a number of different sources
                      [lein-environ "1.0.0"]]

            :ring {:handler      juku.handler/app
                   :uberwar-name "juku.war"}

            :profiles {:dev     {:dependencies [[javax.servlet/servlet-api "2.5"]
                                                [ring-mock "0.1.5"]
                                                ; Midje provides a migration path from clojure.test to a more flexible, readable, abstract, and gracious style of testing.
                                                [midje "1.6.3"]]

                                 :plugins      [; Run multiple leiningen tasks in parallel.
                                                [lein-pdo "0.1.1"]
                                                [lein-midje "3.1.3"]]

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
