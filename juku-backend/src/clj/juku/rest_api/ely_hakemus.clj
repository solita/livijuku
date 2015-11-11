(ns juku.rest-api.ely-hakemus
  (:require [compojure.api.sweet :refer :all]
            [juku.service.ely-hakemus :as service]
            [juku.schema.ely-hakemus :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [juku.schema.common :as sc]))

(defroutes* ely-hakemus-routes
      (GET* "/hakemus/:hakemusid/maararahatarpeet" []
            :auth [:view-hakemus]
            :return [Maararahatarve]
            :path-params [hakemusid :- Long]
            :summary "Hae hakemuksen määrärahatarpeet. Haettava hakemus yksilöidään hakemusid-polkuparametrilla."
            (ok (service/find-hakemus-maararahatarpeet hakemusid)))
      (PUT* "/hakemus/:hakemusid/maararahatarpeet" []
            :auth [:modify-oma-hakemus]
            :audit []
            :return   nil
            :path-params [hakemusid :- Long]
            :body     [maararahatarpeet [Maararahatarve]]
            :summary  "Päivittää hakemuksen määrärahatarpeet."
            (ok (service/save-maararahatarpeet! hakemusid maararahatarpeet)))
      (GET* "/maararahatarvetyypit" []
             :return [sc/Luokka]
             :summary "Hae määrärahatarpeiden tyypit."
            (ok (service/find-maararahatarvetyypit)))

      (GET* "/hakemus/:hakemusid/kehityshankkeet" []
            :auth [:view-hakemus]
            :return [Kehityshanke]
            :path-params [hakemusid :- Long]
            :summary "Hae hakemuksen kehityshankkeet. Haettava hakemus yksilöidään hakemusid-polkuparametrilla."
            (ok (service/find-hakemus-kehityshankkeet hakemusid)))
      (PUT* "/hakemus/:hakemusid/kehityshankkeet" []
            :auth [:modify-oma-hakemus]
            :audit []
            :return   nil
            :path-params [hakemusid :- Long]
            :body     [kehityshankkeet [Kehityshanke]]
            :summary  "Päivittää hakemuksen kehityshankkeet."
            (ok (service/save-kehityshankkeet! hakemusid kehityshankkeet))))

