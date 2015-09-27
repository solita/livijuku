(ns juku.rest-api.seuranta
  (:require [compojure.api.sweet :refer :all]
            [juku.service.seuranta :as service]
            [juku.schema.seuranta :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [juku.schema.common :as sc]))

(defroutes* seuranta-routes
      (GET* "/hakemus/:hakemusid/liikennesuoritteet" []
            :auth [:view-hakemus]
            :return [Liikennesuorite]
            :path-params [hakemusid :- Long]
            :summary "Hae hakemuksen avustuskohteet. Haettava hakemus yksilöidään hakemusid-polkuparametrilla."
            (ok (service/find-hakemus-liikennesuoritteet hakemusid)))
      (PUT* "/hakemus/:hakemusid/liikennesuoritteet" []
            :auth [:modify-oma-hakemus]
            :audit []
            :return   nil
            :path-params [hakemusid :- Long]
            :body     [suoritteet [Liikennesuorite]]
            :summary  "Päivittää tai lisää annetut avustuskohteet."
            (ok (service/save-liikennesuoritteet hakemusid suoritteet)))
      (GET* "/suoritetyypit" []
             :return [sc/Luokka]
             :summary "Hae avustuskohteiden luokittelu: kaikki luokat ja lajit"
            (ok (service/find-suoritetyypit))))

