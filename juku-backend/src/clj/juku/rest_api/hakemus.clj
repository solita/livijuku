(ns juku.rest-api.hakemus
  (:require [compojure.api.sweet :refer :all]
            [juku.service.hakemus :as service]
            [juku.schema.hakemus :refer :all]
            [ring.util.http-response :refer :all]
            [ring.swagger.schema :refer [describe]]
            [schema.core :as s]))

(defroutes* hakemus-routes
      (GET* "/hakemuskaudet" []
            :return [Hakemuskausi]
            :summary "Hae kaikki hakemuskaudet ja niiden hakemukset."
            (ok (service/find-hakemukset-vuosittain)))
      (GET* "/hakemukset/hakija" []
            :return [Hakemuskausi]
            :summary "Hae sisäänkirjautuneen käyttäjän hakemukset hakemuskausittain (vuosittain) ryhmitettynä."
            (ok (service/find-kayttajan-hakemukset-vuosittain)))
      (GET* "/hakemus/:hakemusid" []
            :return Hakemus+
            :path-params [hakemusid :- Long]
            :summary "Hae hakemuksen perustiedot. Haettava hakemus yksilöidään hakemusid-polkuparametrilla."
            (ok (service/get-hakemus-by-id hakemusid)))
      (GET* "/hakemus/avustuskohteet/:hakemusid" []
            :return [Avustuskohde]
            :path-params [hakemusid :- Long]
            :summary "Hae hakemuksen avustuskohteet. Haettava hakemus yksilöidään hakemusid-polkuparametrilla."
            (ok (service/find-avustuskohteet-by-hakemusid hakemusid)))
      (POST* "/hakemus" []
             :return   s/Num
             :body     [hakemus New-Hakemus]
             :summary  "Lisää yksittäinen hakemus."
             (ok (service/add-hakemus! hakemus)))
      (POST* "/avustuskohde" []
             :return   nil
             :body     [avustuskohde Avustuskohde]
             :summary  "Lisää uuden avustuskohteen olemassaolevaan hakemukseen."
             (ok (service/add-avustuskohde! avustuskohde)))
      (PUT* "/avustuskohde" []
             :return   nil
             :body     [avustuskohde Avustuskohde]
             :summary  "Päivittää olemassaolevan avustuskohteen tiedot."
             (ok (service/save-avustuskohde! avustuskohde)))
      (PUT* "/avustuskohteet" []
            :return   nil
            :body     [avustuskohteet [Avustuskohde]]
            :summary  "Päivittää tai lisää annetut avustuskohteet."
            (ok (service/save-avustuskohteet! avustuskohteet)))
      (PUT* "/hakemus/selite" []
            :return   s/Num
            :body-params [hakemusid :- Long, selite :- s/Str]
            :summary  "Päivittää hakemuksen selitteen."
            (ok (service/save-hakemus-selite! hakemusid selite)))
      (POST* "/laheta-hakemus" []
             :return  s/Num
             :body-params     [hakemusid :- Long]
             :summary  "Hakija merkitsee hakemuksen lähetetyksi. Hakemus on tämän jälkeen vireillä."
             (ok (service/laheta-hakemus! hakemusid)))
      (POST* "/tarkasta-hakemus" []
             :return  s/Num
             :body-params     [hakemusid :- Long]
             :summary  "Käsittelijä merkitsee hakemuksen tarkastetuksi."
             (ok (service/tarkasta-hakemus! hakemusid)))
      (POST* "/hakemuskausi" []
             :return   nil
             :body-params     [vuosi :- s/Int]
             :summary  "Avaa uusi hakemuskausi."
             (ok (service/avaa-hakemuskausi! vuosi))))

