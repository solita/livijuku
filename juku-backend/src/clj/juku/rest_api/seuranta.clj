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
            :summary "Hae hakemuksen liikennesuoritteet. Haettava hakemus yksilöidään hakemusid-polkuparametrilla."
            (ok (service/find-hakemus-liikennesuoritteet hakemusid)))
      (PUT* "/hakemus/:hakemusid/liikennesuoritteet" []
            :auth [:modify-oma-hakemus]
            :audit []
            :return   nil
            :path-params [hakemusid :- Long]
            :body     [suoritteet [Liikennesuorite]]
            :summary  "Päivittää hakemuksen liikennesuoritteet."
            (ok (service/save-liikennesuoritteet! hakemusid suoritteet)))
      (GET* "/suoritetyypit" []
             :return [sc/Luokka]
             :summary "Hae liikennesuoritteen suoritetyypit."
            (ok (service/find-suoritetyypit)))

      (GET* "/hakemus/:hakemusid/lippusuoritteet" []
            :auth [:view-hakemus]
            :return [Lippusuorite]
            :path-params [hakemusid :- Long]
            :summary "Hae hakemuksen lippusuoritteet. Haettava hakemus yksilöidään hakemusid-polkuparametrilla."
            (ok (service/find-hakemus-lippusuoritteet hakemusid)))
      (PUT* "/hakemus/:hakemusid/lippusuoritteet" []
            :auth [:modify-oma-hakemus]
            :audit []
            :return   nil
            :path-params [hakemusid :- Long]
            :body     [suoritteet [Lippusuorite]]
            :summary  "Päivittää hakemuksen lippusuoritteet."
            (ok (service/save-lippusuoritteet! hakemusid suoritteet)))
      (GET* "/lipputyypit" []
            :return [sc/Luokka]
            :summary "Hae lipputyypit."
            (ok (service/find-lipputyypit))))

