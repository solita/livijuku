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

(s/defschema Settings {:server Server
                       :db Db
                       :asiahallinta (s/either Service (s/eq "off"))})

(def default-settings {
           :server {:port 8082}
           :db {
              :url "jdbc:oracle:thin:@localhost:1521:orcl"
              :user "juku_app"
              :password "juku"}
           :asiahallinta {
                :url "http://asha.livijuku.solita.fi/api"
                :user "test"
                :password "test"}})

(def settings (read-settings (io/file (or (env :properties-file) "./juku.properties")) default-settings Settings))

