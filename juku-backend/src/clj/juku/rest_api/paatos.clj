(ns juku.rest-api.paatos
  (:require [compojure.api.sweet :refer :all]
            [juku.service.paatos :as service]
            [juku.schema.paatos :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]))

(defroutes* paatos-routes
    (GET* "/hakemus/:hakemusid/paatos" []
          :auth [:view-hakemus]
          :return (s/maybe Paatos)
          :path-params [hakemusid :- Long]
          :summary "Hae hakemuksen nykyinen päätös."
          (ok (service/find-current-paatos hakemusid)))

    (GET* "/hakemus/:hakemusid/paatos/pdf*" []
          :auth [:view-hakemus]
          :path-params [hakemusid :- Long]
          :summary "Hae hakemuksen (hakemusid) nykyisen ratkaisun päätösasiakirja."
          (content-type (ok (service/find-paatos-pdf hakemusid))
                        "application/pdf"))

    (PUT* "/hakemus/:hakemusid/paatos" []
          :auth [:kasittely-hakemus]
          :audit [:body-params]
          :return  nil
          :path-params [hakemusid :- Long]
          :body [paatos EditPaatos]
          :summary "Tallenna hakemuksen nykyisen päätöksen tiedot."
          (ok (service/save-paatos! paatos)))

    (POST* "/hakemus/:hakemusid/hyvaksy-paatos" []
           :auth [:hyvaksy-paatos]
           :audit []
           :return  nil
           :path-params [hakemusid :- Long]
           :summary "Hyväksy hakemuksen avoinna oleva päätös."
           (ok (service/hyvaksy-paatos! hakemusid)))

    (POST* "/hakemus/:hakemusid/peruuta-paatos" []
           :auth [:hyvaksy-paatos]
           :audit []
           :return  nil
           :path-params [hakemusid :- Long]
           :summary "Peruuta hakemuksen hyväksytty päätös."
           (ok (service/peruuta-paatos! hakemusid))))