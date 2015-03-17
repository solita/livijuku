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
          (ok (service/find-current-paatos hakemusid)))

    (GET* "/hakemus/:hakemusid/paatos/:paatosnumero/pdf" []
          :path-params [hakemusid :- Long, paatosnumero :- Long]
          :summary "Hae hakemuksen (hakemusid) päätöksen (päätösnumero) päätösasiakirja."
          (content-type (ok (service/paatos-pdf hakemusid paatosnumero))
                        "application/pdf"))

    (PUT* "/hakemus/:hakemusid/paatos" []
          :return  nil
          :path-params [hakemusid :- Long]
          :body [paatos EditPaatos]
          :summary "Tallenna hakemuksen nykyisen päätöksen tiedot."
          (ok (service/save-paatos! paatos)))

    (POST* "/hakemus/:hakemusid/hyvaksy-paatos" []
          :return  nil
          :path-params [hakemusid :- Long]
          :summary "Hyväksy hakemuksen avoinna oleva päätös."
          (ok (service/hyvaksy-paatos! hakemusid))))
