(ns juku.reitit
  (:require [compojure.core :as c]
            [compojure.route :as r]
            [schema.core :as s]
            [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [juku.rest-api.hakemus :refer [hakemus-routes]]
            [juku.rest-api.organisaatio :refer [organisaatio-routes]]
            [juku.schema.organisaatio :refer :all]
            [juku.schema.hakemus :refer :all]
            [environ.core :refer [env]]))

(c/defroutes notfound (r/not-found "Not Found"))

(defapi juku-api
    (swagger-ui "/api/ui")
    (swagger-docs
      :title "Liikennevirasto - Juku API"
      :description "Joukkoliikenteen avustushakemusten hallintaan ja hakuihin liittyvät palvelut")
    (swaggered "hakemus"
               :description "Hakemus API"
               hakemus-routes)
    (swaggered "organisaatio"
               :description "Organisaatio API"
               organisaatio-routes)
    notfound)