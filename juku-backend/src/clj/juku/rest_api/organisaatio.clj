(ns juku.rest-api.organisaatio
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [juku.service.organisaatio :as service]
            [juku.schema.organisaatio :refer :all]))

(defroutes* organisaatio-routes
      (GET* "/organisaatiot" []
            :return [Organisaatio]
            :summary "Hae kaikki organisaatiot"
            (ok (service/organisaatiot))))

