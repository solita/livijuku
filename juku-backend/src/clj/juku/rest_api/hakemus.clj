(ns juku.rest-api.hakemus
  (:require [compojure.api.sweet :refer :all]
            [juku.db.hakemus :as db]
            [juku.schema.hakemus :refer :all]
            [ring.util.http-response :refer :all]
            [ring.swagger.schema :refer [describe]]))

(defroutes* hakemus-routes
      (GET* "/hakemukset/hakija" []
            :return [Hakemuskausi]
            :query-params [osastoid :- Long]
            (ok (db/find-osaston-hakemukset-vuosittain osastoid))))

