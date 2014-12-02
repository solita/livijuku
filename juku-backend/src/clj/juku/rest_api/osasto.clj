(ns juku.rest-api.osasto
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [juku.service.osasto :as service]
            [juku.schema.osasto :refer :all]))

(defroutes* osasto-routes
      (GET* "/osastot" []
            :return [Osasto]
            :summary "Hae kaikki osastot"
            (ok (service/osastot))))

