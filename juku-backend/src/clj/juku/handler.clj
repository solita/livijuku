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
            [juku.middleware :as jm]
            [common.collection :as f]
            [juku.db.oracle-metrics :as metrics]))

(c/defroutes notfound (r/not-found "Not Found"))

(defapi juku-api
        (swagger-ui "/api/ui")
        (swagger-docs
          :title "Liikennevirasto - Juku API"
          :apiVersion "0.0.1"
          :license "Euroopan unionin yleinen lisenssi v.1.1"
          :licenseUrl "http://ec.europa.eu/idabc/servlets/Doc7ace.pdf?id=31982"
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

(def app (-> juku-api
  jm/wrap-user
  (m/wrap-defaults (assoc-in m/site-defaults [:security :anti-forgery] false))))

;; set oracle metrics to all service namespaces excluding yesql generated functions
(doseq [service (filter (f/starts-with (comp name ns-name) "juku.service") (all-ns))]
  (metrics/trace-ns service (comp not :yesql.generate/source)))
