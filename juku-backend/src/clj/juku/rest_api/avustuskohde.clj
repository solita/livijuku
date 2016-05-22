(ns juku.rest-api.avustuskohde
  (:require [compojure.api.sweet :refer :all]
            [juku.service.avustuskohde :as service]
            [juku.schema.hakemus :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]))

(defroutes avustuskohde-routes
      (GET "/hakemus/avustuskohteet/:hakemusid" []
            :auth [:view-hakemus]
            :return [Avustuskohde+alv]
            :path-params [hakemusid :- Long]
            :summary "Hae hakemuksen avustuskohteet. Haettava hakemus yksilöidään hakemusid-polkuparametrilla."
            (ok (service/find-avustuskohteet hakemusid)))
      (PUT "/avustuskohteet" []
            :auth [:modify-oma-hakemus]
            :audit []
            :return   nil
            :body     [avustuskohteet [Avustuskohde]]
            :summary  "Päivittää tai lisää annetut avustuskohteet."
            (ok (service/save-avustuskohteet! avustuskohteet)))
      (GET "/avustuskohdeluokittelu" []
             :return [Avustuskohdeluokka]
             :summary "Hae avustuskohteiden luokittelu: kaikki luokat ja lajit"
            (ok (service/avustuskohde-luokittelu))))

