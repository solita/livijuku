(ns juku.rest-api.hakemus
  (:require [compojure.api.sweet :refer :all]
            [juku.service.hakemus :as db]
            [juku.schema.hakemus :refer :all]
            [ring.util.http-response :refer :all]
            [ring.swagger.schema :refer [describe]]
            [schema.core :as s]))

(defroutes* hakemus-routes
      (GET* "/hakemukset/hakija/:organisaatioid" []
            :return [Hakemuskausi]
            :path-params [organisaatioid :- Long]
            :summary "Hae hakijan hakemukset hakemuskausittain (vuosittain) ryhmitettynä."
            (ok (db/find-organisaation-hakemukset-vuosittain organisaatioid)))
      (GET* "/hakemus/:hakemusid" []
            :return [Hakemus]
            :path-params [hakemusid :- Long]
            :summary "Hae hakemuksen perustiedot. Haettava hakemus yksilöidään hakemusid-polkuparametrilla."
            (ok (db/get-hakemus-by-id hakemusid)))
      (GET* "/hakemus/avustuskohteet/:hakemusid" []
            :return [Hakemus]
            :path-params [hakemusid :- Long]
            :summary "Hae hakemuksen avustuskohteet. Haettava hakemus yksilöidään hakemusid-polkuparametrilla."
            (ok (db/find-avustuskohteet-by-hakemusid hakemusid)))
      (POST* "/hakemus" []
             :return   s/Num
             :body     [hakemus New-Hakemus]
             :summary  "Lisää yksittäinen hakemus"
             (ok (db/add-hakemus! hakemus)))
      (POST* "/hakemuskausi" []
             :return   nil
             :form-params     [vuosi :- s/Int]
             :summary  "Avaa uusi hakemuskausi"
             (ok (db/avaa-hakemuskausi! vuosi))))

