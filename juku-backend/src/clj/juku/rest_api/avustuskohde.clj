(ns juku.rest-api.avustuskohde
  (:require [compojure.api.sweet :refer :all]
            [juku.service.avustuskohde :as service]
            [juku.schema.hakemus :refer :all]
            [ring.util.http-response :refer :all]
            [ring.swagger.schema :refer [describe]]
            [schema.core :as s]))

(defroutes* avustuskohde-routes
      (GET* "/hakemus/avustuskohteet/:hakemusid" []
            :return [Avustuskohde]
            :path-params [hakemusid :- Long]
            :summary "Hae hakemuksen avustuskohteet. Haettava hakemus yksilöidään hakemusid-polkuparametrilla."
            (ok (service/find-avustuskohteet-by-hakemusid hakemusid)))
      (POST* "/avustuskohde" []
             :return   nil
             :body     [avustuskohde Avustuskohde]
             :summary  "Lisää uuden avustuskohteen olemassaolevaan hakemukseen."
             (ok (service/add-avustuskohde! avustuskohde)))
      (PUT* "/avustuskohde" []
             :return   nil
             :body     [avustuskohde Avustuskohde]
             :summary  "Päivittää avustuskohteen tiedot tai lisää uuden avustuskohteen."
             (ok (service/save-avustuskohde! avustuskohde)))
      (PUT* "/avustuskohteet" []
            :return   nil
            :body     [avustuskohteet [Avustuskohde]]
            :summary  "Päivittää tai lisää annetut avustuskohteet."
            (ok (service/save-avustuskohteet! avustuskohteet)))
      (GET* "/avustuskohdeluokittelu" []
             :return [Avustuskohdeluokka]
             :summary "Hae avustuskohteiden luokittelu: kaikki luokat ja lajit"
            (ok (service/avustuskohde-luokittelu))))

