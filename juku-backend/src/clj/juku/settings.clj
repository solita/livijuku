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

(s/defschema Asiahallinta (assoc Service :omistavahenkilo s/Str))

(s/defschema Settings {:server Server
                       :liite-max-size s/Num
                       :db Db
                       :asiahallinta (s/either Asiahallinta (s/eq "off"))})

(def default-settings {
           :server {:port 8082}
           :liite-max-size 52428800
           :db {
              :url "jdbc:oracle:thin:@localhost:1521:orcl"
              :user "juku_app"
              :password "juku"}
           :asiahallinta {
                :url "http://asha.livijuku.solita.fi/api"
                :user "test"
                :password "test"
                ;; asioiden omistavan henkilön käyttäjätunnus
                :omistavahenkilo "test"}})

(def settings (read-settings (io/file (or (env :properties-file) "./juku.properties")) default-settings Settings))

