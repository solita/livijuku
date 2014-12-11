(defproject juku-backend "0.1.0-SNAPSHOT"
            :description "Liikennevirasto - Joukkoliikenteen rahoitus-, kustannus- ja suoritetietojen keräys- ja seurantajärjestelmä"
            :url "http://example.com/FIXME"
            :min-lein-version "2.4.3"

            :dependencies [[org.clojure/clojure "1.6.0"]
                           [org.clojure/java.jdbc "0.3.6"]

                           [compojure "1.2.1"]
                           [metosin/compojure-api "0.16.6"]
                           [metosin/ring-http-response "0.5.2"]
                           [metosin/ring-swagger "0.15.0"]
                           [metosin/ring-swagger-ui "2.0.17"]
                           [org.clojars.zentrope/ojdbc "11.2.0.3.0"]
                           [yesql "0.5.0-beta2"]
                           [prismatic/schema "0.3.3"]
                           [clj-time "0.8.0"]
                           [cheshire "5.3.1"]
                           [ring "1.3.1"]
                           [ring/ring-defaults "0.1.2"]
                           [http-kit "2.1.10"]
                           [org.clojure/tools.logging "0.3.1"]
                           [ch.qos.logback/logback-classic "1.1.2"]
                           [com.google.guava/guava "18.0"]
                           [com.zaxxer/HikariCP-java6 "2.2.5"]
                           ;; Clojure i18n & L10n library
                           [com.taoensso/tower "3.0.2"]
                           ;; Clojurescript i18n & L10n library, compatible with tower
                           [net.unit8/tower-cljs "0.1.0"]
                           ;; Library for managing environment variables in Clojure.
                           [environ "1.0.0"]
                           ;; Timbre brings functional, Clojure-y goodness to all your logging needs.
                           [com.taoensso/timbre "3.3.1"]
                           [enlive "1.1.5"]]

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
