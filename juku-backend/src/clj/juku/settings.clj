(ns juku.settings
  (:require [schema.core :as s]
            [common.settings :refer [read-settings]]
            [clojure.java.io :as io]
            [environ.core :refer [env]]))

(s/defschema Server {:port s/Int} )

(s/defschema Db {:url         s/Str
                 :user        s/Str
                 :password    s/Str})

(s/defschema Service {:url         s/Str
                      :user        s/Str
                      :password    s/Str})

(s/defschema Web {:url            s/Str
                  :context-path   s/Str
                  :api-path       s/Str})

(s/defschema Email {:from         s/Str
                    :server       s/Str
                    :port         s/Int})

(s/defschema Asiahallinta (assoc Service :omistavahenkilo s/Str))

(s/defschema Settings {:server Server
                       :liite-max-size s/Num
                       :web Web
                       :db Db
                       :email (s/conditional map? Email string? (s/eq "off"))
                       :asiahallinta (s/conditional map? Asiahallinta string? (s/eq "off"))})

(def default-settings {
           :server {:port 8080}
           :liite-max-size 52428800
           :web {
             :url "http://localhost:9000"
             :context-path ""
             :api-path "/"}
           :db {
              :url "jdbc:oracle:thin:@localhost:1521/orclpdb1.localdomain"
              :user "juku_app"
              :password "juku"}
           :email {
              :from "livijuku@solita.fi"
              :server "localhost"
              :port 2525}
           :asiahallinta {
              :url "http://asha.livijuku.solita.fi/api"
              :user "test"
              :password "test"
              ;; asioiden omistavan henkilön käyttäjätunnus
              :omistavahenkilo "test"}})

(def settings (read-settings (io/file (or (env :properties-file) "./juku.properties")) default-settings Settings))

(defn asiahallinta-on? [] (not= (:asiahallinta settings) "off"))

