(ns juku.rest-api.hakemus
  (:require [compojure.api.sweet :refer :all]
            [juku.db.hakemus :as db]
            [juku.schema.hakemus :refer :all]
            [ring.util.http-response :refer :all]
            [ring.swagger.schema :refer [describe]]
            [schema.core :as s]))

(defroutes* hakemus-routes
      (GET* "/hakemukset/hakija" []
            :return [Hakemuskausi]
            :query-params [osastoid :- Long]
            :summary "Hae hakijan hakemukset"
            (ok (db/find-osaston-hakemukset-vuosittain osastoid)))
      (POST* "/add-hakemus" []
             :return   s/Num
             :body     [hakemus New-Hakemus]
             :summary  "Lisää hakemus"
             (ok (db/add-hakemus! hakemus))))

