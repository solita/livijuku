(ns juku.rest-api.tilastot
  (:require [compojure.api.sweet :refer :all]
            [juku.service.tilastot :as service]
            [juku.schema.tunnusluku :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [juku.schema.common :as sc]))

(defroutes* tilastot-routes

  (GET* "/tilastot/nousut" []
      :auth [:view-tunnusluvut]
      :return [s/Any]
      :summary "Testipalvelu käyttöliittymäratkaisun testaukseen."
    (ok (service/find-nousut))))

