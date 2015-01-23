(ns juku.rest-api.paatos
  (:require [compojure.api.sweet :refer :all]
            [juku.service.paatos :as service]
            [juku.schema.paatos :refer :all]
            [ring.util.http-response :refer :all]
            [ring.swagger.schema :refer [describe]]
            [schema.core :as s]))

(defroutes* paatos-routes
    (GET* "/hakemus/:hakemusid/paatos" []
          :return (s/maybe Paatos)
          :path-params [hakemusid :- Long]
          :summary "Hae hakemuksen nykyinen päätös."
          (ok (service/find-paatos hakemusid)))

    (PUT* "/hakemus/:hakemusid/paatos" []
          :return  nil
          :path-params [hakemusid :- Long]
          :body [paatos Edit-Paatos]
          :summary "Tallenna hakemuksen päätöksen tiedot."
          (ok (service/save-paatos! paatos)))

    (POST* "/hakemus/:hakemusid/hyvaksy-paatos" []
          :return  nil
          :path-params [hakemusid :- Long]
          :summary "Hyväksy hakemuksen avoinna oleva päätös."
          (ok (service/hyvaksy-paatos! hakemusid))))
