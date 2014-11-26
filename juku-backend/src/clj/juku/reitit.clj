(ns juku.reitit
  (:require [clojure.pprint :refer [pprint]]
            [clojure.java.io :as io]
            [compojure.core :as c]
            [compojure.route :as r]
            [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [juku.schema.hakemus :refer :all]
            [juku.rest-api.hakemus :refer :all]
            [ring.middleware.resource :refer [wrap-resource]]
            [net.cgrand.enlive-html :as enlive]
            [clojure.java.io :as io]
            [environ.core :refer [env]]))


(c/defroutes notfound (r/not-found "Not Found"))

(defapi hakemus-api
    (swagger-ui "/api/ui")
    (swagger-docs
      :title "Liikennevirasto - Juku API"
      :description "Joukkoliikenteen avustushakemusten hallintaan ja hakuihin liittyvät palvelut")
    (swaggered "hakemus"
               :description "Hakemus API"
               hakemus-routes)
    notfound)