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
      :path-params [vuosi :- Long
                    organisaatioid :- Long
                    sopimustyyppitunnus :- s/Str]
      :summary "Hae organisaation liikenteen markkinatiedot tietylle vuodella ja sopimustyypille."
    (ok (service/find-liikennevuositilasto vuosi organisaatioid sopimustyyppitunnus)))
  (PUT* "/liikennetilastot/:vuosi/:organisaatioid/:sopimustyyppitunnus" []
      :auth [:view-hakemus]
      :return [Liikennekuukausi]
      :path-params [vuosi :- Long
                organisaatioid :- Long
                sopimustyyppitunnus :- s/Str]
      :body [kuukaudet [Liikennekuukausi]]
      :summary "Tallenna liikenteen markkinatiedot tietylle vuodella ja sopimustyypille."
      (ok (service/save-liikennevuositilasto! vuosi organisaatioid sopimustyyppitunnus kuukaudet)))

  (GET* "/liikenneviikko/:vuosi/:organisaatioid/:sopimustyyppitunnus" []
      :auth [:view-hakemus]
      :return [Liikennepaiva]
      :path-params [vuosi :- Long
                    organisaatioid :- Long
                    sopimustyyppitunnus :- s/Str]
      :summary "Hae organisaation liikenteen talviviikon markkinatiedot tietylle vuodella ja sopimustyypille."
      (ok (service/find-liikenneviikkotilasto vuosi organisaatioid sopimustyyppitunnus)))
  (PUT* "/liikenneviikko/:vuosi/:organisaatioid/:sopimustyyppitunnus" []
      :auth [:view-hakemus]
      :return [Liikennepaiva]
      :path-params [vuosi :- Long
              organisaatioid :- Long
              sopimustyyppitunnus :- s/Str]
      :body [viikko [Liikennepaiva]]
      :summary "Tallenna organisaation talviviikon markkinatiedot tietylle vuodella ja sopimustyypille."
      (ok (service/save-liikenneviikkotilasto! vuosi organisaatioid sopimustyyppitunnus viikko))))

