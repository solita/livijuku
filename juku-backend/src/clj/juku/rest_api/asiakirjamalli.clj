(ns juku.rest-api.asiakirjamalli
  (:require [compojure.api.sweet :refer :all]
            [juku.service.asiakirjamalli :as service]
            [juku.rest-api.response :as response]
            [juku.schema.asiakirjamalli :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [clojure.java.io :as io]))

(defroutes asiakirjamalli-routes
    (GET "/asiakirjamallit" []
          :auth [:view-hakemus]
          :return [Asiakirjamalli]
          :summary "Hae kaikki asiakirjamallit."
          (ok (service/find-all-asiakirjamallit))))
