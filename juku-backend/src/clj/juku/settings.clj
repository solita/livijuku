(ns juku.settings
  (:require [schema.core :as s]
            [common.settings :refer [read-settings]]
            [clojure.java.io :as io]))

(s/defschema Server {:port s/Int} )

(s/defschema Db {:url         s/Str
                 :user        s/Str
                 :password    s/Str})

(s/defschema Settings {:server Server
                       :db Db})

(def default-settings {
           :server {:port 8082}
           :db {
              :url "jdbc:oracle:thin:@localhost:1521:orcl"
              :user "juku_app"
              :password "juku"}})

(def settings (read-settings (io/file "./juku.properties") default-settings Settings))

