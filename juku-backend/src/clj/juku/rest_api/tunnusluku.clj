(ns juku.rest-api.tunnusluku
  (:require [compojure.api.sweet :refer :all]
            [juku.service.tunnusluku :as service]
            [juku.schema.tunnusluku :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [juku.schema.common :as sc]))

(defroutes* tunnusluku-routes
      (GET* "/liikennetilastot/:vuosi/:organisaatioid/:sopimustyyppitunnus" []
            :auth [:view-hakemus]
            :return [Liikennekuukausi]
            :path-params [vuosi :- Integer
                          organisaatioid :- Long
                          sopimustyyppitunnus :- s/Str]
            :summary "Hae organisaation liikenteen markkinatiedot tietylle vuodella ja sopimustyypille."
            (ok (service/find-liikennevuositilasto vuosi organisaatioid sopimustyyppitunnus)))
      (PUT* "/liikennetilastot/:vuosi/:organisaatioid/:sopimustyyppitunnus" []
            :auth [:view-hakemus]
            :return [Liikennekuukausi]
            :path-params [vuosi :- Integer
                          organisaatioid :- Long
                          sopimustyyppitunnus :- s/Str]
            :body [kuukaudet [Liikennekuukausi]]
            :summary "Hae organisaation liikenteen markkinatiedot tietylle vuodella ja sopimustyypille."
            (ok (service/save-organisaatio-liikennetilasto! vuosi organisaatioid sopimustyyppitunnus kuukaudet))))

