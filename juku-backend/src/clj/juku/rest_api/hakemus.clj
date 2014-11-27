(ns juku.rest-api.hakemus
  (:require [compojure.api.sweet :refer :all]
            [juku.service.hakemus :as db]
            [juku.schema.hakemus :refer :all]
            [ring.util.http-response :refer :all]
            [ring.swagger.schema :refer [describe]]
            [schema.core :as s]))

(defroutes* hakemus-routes
      (GET* "/hakemukset/hakija/:osastoid" []
            :return [Hakemuskausi]
            :path-params [osastoid :- Long]
            :summary "Hae hakijan hakemukset hakemuskausittain (vuosittain) ryhmitettynä"
            (ok (db/find-osaston-hakemukset-vuosittain osastoid)))
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

