(ns juku.handler
  (:require [compojure.core :as c]
            [compojure.route :as r]
            [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]

            [juku.rest-api.hakemus :refer [hakemus-routes]]
            [juku.rest-api.avustuskohde :refer [avustuskohde-routes]]
            [juku.rest-api.hakemuskausi :refer [hakemuskausi-routes]]
            [juku.rest-api.paatos :refer [paatos-routes]]
            [juku.rest-api.liitteet :refer [liitteet-routes]]
            [juku.rest-api.organisaatio :refer [organisaatio-routes]]
            [juku.rest-api.user :refer [user-routes]]

            [ring.middleware.defaults :as m]
            [juku.middleware :as jm]
            [common.collection :as f]
            [juku.db.oracle-metrics :as metrics]))

(c/defroutes notfound (r/not-found "Not Found"))

(defapi juku-api {:exceptions {:exception-handler jm/exception-handler}}
        (swagger-ui "/api/ui")
        (swagger-docs :info {
            :title "Liikennevirasto - Juku API"
            :version "0.0.1"
            :description "Joukkoliikenteen avustushakemusten hallintaan ja hakuihin liittyvät palvelut"
            :license {
              :name "Euroopan unionin yleinen lisenssi v.1.1"
              :url "http://ec.europa.eu/idabc/servlets/Doc7ace.pdf?id=31982"}})
      (middlewares [jm/wrap-user]
        (context* "" [] :tags ["Hakemuskausi API"] hakemuskausi-routes)
        (context* "" [] :tags ["Hakemus API"] hakemus-routes)
        (context* "" [] :tags ["Avustuskohde API"] avustuskohde-routes)
        (context* "" [] :tags ["Päätös API"] paatos-routes)
        (context* "" [] :tags ["Liite API"] liitteet-routes)
        (context* "" [] :tags ["Organisaatio API"] organisaatio-routes)
        (context* "" [] :tags ["Käyttäjä API"] user-routes)
        notfound))

(def app (-> juku-api
  (m/wrap-defaults (assoc-in m/site-defaults [:security :anti-forgery] false))))

;; set oracle metrics to all service namespaces excluding yesql generated functions
(doseq [service (filter (f/starts-with (comp name ns-name) "juku.service") (all-ns))]
  (metrics/trace-ns service (comp not :yesql.generate/source)))
