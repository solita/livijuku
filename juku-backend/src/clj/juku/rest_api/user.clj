(ns juku.rest-api.user
  (:require [compojure.api.sweet :refer :all]
            [juku.service.user :as service]
            [juku.schema.user :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]))

(defroutes* user-routes
    (GET* "/user" []
          :return User+Privileges
          :summary "Hae nykyisen käyttäjän tiedot."
          (ok service/*current-user*))
    (GET* "/organisaatio/:organisaatioid/users" []
          :return [User]
          :path-params [organisaatioid :- Long]
          :summary "Hae organisaation kaikki käyttäjät."
          (ok (service/find-users-by-organization organisaatioid))))