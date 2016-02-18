(ns juku.handler
  (:require [compojure.core :as c]
            [compojure.route :as r]
            [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [compojure.api.exception :as ex]

            [juku.rest-api.authorization]
            [juku.rest-api.hakemus :refer [hakemus-routes]]
            [juku.rest-api.avustuskohde :refer [avustuskohde-routes]]
            [juku.rest-api.hakemuskausi :refer [hakemuskausi-routes]]
            [juku.rest-api.paatos :refer [paatos-routes]]
            [juku.rest-api.liitteet :refer [liitteet-routes]]
            [juku.rest-api.organisaatio :refer [organisaatio-routes]]
            [juku.rest-api.seuranta :refer [seuranta-routes]]
            [juku.rest-api.ely-hakemus :refer [ely-hakemus-routes]]
            [juku.rest-api.tunnusluku :refer [tunnusluku-routes]]
            [juku.rest-api.user :refer [user-routes]]
            [juku.rest-api.tilastot :refer [tilastot-routes]]

            [ring.middleware.defaults :as m]
            [juku.middleware :as jm]
            [common.collection :as f]
            [juku.db.oracle-metrics :as metrics]))

(c/defroutes notfound (r/not-found "Not Found"))

(defapi juku-api
        {:exceptions
          {:handlers
           {:compojure.api.exception/request-parsing
              (jm/logging-wrapper "400 bad request - parse error:" ex/request-parsing-handler)
            :compojure.api.exception/request-validation
              (jm/logging-wrapper "400 bad request - validation error:" ex/request-validation-handler)
            :compojure.api.exception/response-validation
              (jm/logging-wrapper "500 system error (bad response) - validation error:" jm/response-validation-handler)
            :compojure.api.exception/default jm/exception-handler}}
         :format
          {:formats [:json jm/wrap-csv-params]
           :params-opts {jm/wrap-csv-params {:delimiter \;}}}}

        (swagger-ui "/api/ui")
        (swagger-docs :info {
            :title "Liikennevirasto - Juku API"
            :version "1.4.0"
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
        (context* "" [] :tags ["Seuranta API"] seuranta-routes)
        (context* "" [] :tags ["ELY hakemus API"] ely-hakemus-routes)
        (context* "" [] :tags ["Käyttäjä API"] user-routes)
        (context* "" [] :tags ["Tunnusluku API"] tunnusluku-routes)
        (context* "" [] :tags ["Tilastot API"] tilastot-routes)
        notfound))

(def app (-> juku-api
             (jm/wrap-double-submit-cookie [#"GET /hakemuskausi/.*/hakuohje"
                                            #"GET /hakemuskausi/.*/elyhakuohje"
                                            #"GET /hakemus/.*/pdf.*"
                                            #"GET /hakemus/.*/liite/.*"
                                            #"GET /hakemus/.*/paatos/pdf.*"
                                            #"GET /api/ui/.*"
                                            #"GET /swagger.json"

                                            #"POST /hakemuskausi/.*/hakuohje"
                                            #"POST /hakemuskausi/.*/elyhakuohje"
                                            #"POST /hakemus/.*/liite"])
            (m/wrap-defaults (assoc-in m/site-defaults [:security :anti-forgery] false))
             jm/wrap-no-cache))

;; set oracle metrics to all service namespaces excluding yesql generated functions
(doseq [service (filter (f/starts-with (comp name ns-name) "juku.service") (all-ns))]
  (metrics/trace-ns service (comp not :yesql.generate/source)))
