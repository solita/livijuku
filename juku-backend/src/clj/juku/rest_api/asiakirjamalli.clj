(ns juku.rest-api.asiakirjamalli
  (:require [compojure.api.sweet :refer :all]
            [juku.service.asiakirjamalli :as service]
            [juku.rest-api.response :as response]
            [juku.schema.asiakirjamalli :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [clojure.java.io :as io]))

(defroutes asiakirjamalli-routes
   (GET "/asiakirjamallit" []
        :auth [:view-hakemus]
        :return [Asiakirjamalli]
        :summary "Hae kaikki asiakirjamallit."
        (ok (service/find-all)))

   (GET "/asiakirjamalli/:id" []
     :path-params [id :- Long]
     :auth [:view-hakemus]
     :return Asiakirjamalli+sisalto
     :summary "Hae yksittäinen asiakirjamalli."
     (ok (service/find-by-id id)))

   (PUT "/asiakirjamalli/:id" []
     :path-params [id :- Long]
     :body [asiakirjamalli Edit-Asiakirjamalli]
     :auth [:view-hakemus]
     :return Long
     :summary "Päivitä yksittäinen asiakirjamalli."
     (ok (service/edit-asiakirjamalli! id asiakirjamalli)))

   (POST "/asiakirjamalli" []
     :body [asiakirjamalli Edit-Asiakirjamalli]
     :auth [:view-hakemus]
     :return Long
     :summary "Luo uusi asiakirjamalli."
     (ok (service/add-asiakirjamalli! asiakirjamalli)))

   (DELETE "/asiakirjamalli/:id" []
     :path-params [id :- Long]
     :auth [:view-hakemus]
     :return Long
     :summary "Päivitä yksittäinen asiakirjamalli."
     (ok (service/delete-asiakirjamalli! id))))
