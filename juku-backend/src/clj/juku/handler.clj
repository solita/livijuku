(ns juku.handler
  (:require [compojure.core :as c]
            [compojure.route :as r]
            [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]

            [juku.rest-api.hakemus :refer [hakemus-routes]]
            [juku.rest-api.organisaatio :refer [organisaatio-routes]]
            [juku.rest-api.user :refer [user-routes]]

            [schema.core :as s]
            [juku.schema.organisaatio :refer :all]
            [juku.schema.hakemus :refer :all]
            [juku.schema.user :refer :all]

            [ring.middleware.defaults :as m]
            [juku.middleware :as jm]))

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
        (swaggered "user"
                   :description "Käyttäjä API"
                   user-routes)
        notfound)

(def app (-> #'juku-api
  jm/wrap-user
  (m/wrap-defaults (assoc-in m/site-defaults [:security :anti-forgery] false))))









