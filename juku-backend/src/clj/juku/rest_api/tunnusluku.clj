(ns juku.rest-api.tunnusluku
  (:require [compojure.api.sweet :refer :all]
            [juku.service.tunnusluku :as service]
            [juku.schema.tunnusluku :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [juku.schema.common :as sc]))

(defroutes*
  tunnusluku-routes

  ;; Vuoden liikennetilastot kuukausitasolla
  (GET* "/liikennetilastot/:vuosi/:organisaatioid/:sopimustyyppitunnus" []
      :auth [:view-tunnusluvut]
      :return [Liikennekuukausi]
      :path-params [vuosi :- Long
                    organisaatioid :- Long
                    sopimustyyppitunnus :- s/Str]
      :summary "Hae organisaation liikenteen kysyntä ja tarjontatiedot tietylle vuodella ja sopimustyypille."
    (ok (service/find-liikennevuositilasto vuosi organisaatioid sopimustyyppitunnus)))
  (PUT* "/liikennetilastot/:vuosi/:organisaatioid/:sopimustyyppitunnus" []
      :auth [:view-tunnusluvut]
      :return nil
      :path-params [vuosi :- Long
                organisaatioid :- Long
                sopimustyyppitunnus :- s/Str]
      :body [kuukaudet [Liikennekuukausi]]
      :summary "Tallenna liikenteen kysyntä ja tarjontatiedot tietylle vuodella ja sopimustyypille."
      (ok (service/save-liikennevuositilasto! vuosi organisaatioid sopimustyyppitunnus kuukaudet)))

  ;; Liikenneviikko
  (GET* "/liikenneviikko/:vuosi/:organisaatioid/:sopimustyyppitunnus" []
      :auth [:view-tunnusluvut]
      :return [Liikennepaiva]
      :path-params [vuosi :- Long
                    organisaatioid :- Long
                    sopimustyyppitunnus :- s/Str]
      :summary "Hae organisaation liikenteen talviviikon kysyntä ja tarjontatiedot tietylle vuodella ja sopimustyypille."
      (ok (service/find-liikenneviikkotilasto vuosi organisaatioid sopimustyyppitunnus)))
  (PUT* "/liikenneviikko/:vuosi/:organisaatioid/:sopimustyyppitunnus" []
      :auth [:view-tunnusluvut]
      :return nil
      :path-params [vuosi :- Long
              organisaatioid :- Long
              sopimustyyppitunnus :- s/Str]
      :body [viikko [Liikennepaiva]]
      :summary "Tallenna organisaation talviviikon kysyntä ja tarjontatiedot tietylle vuodella ja sopimustyypille."
      (ok (service/save-liikenneviikkotilasto! vuosi organisaatioid sopimustyyppitunnus viikko)))

  ;; Kalusto
  (GET* "/kalusto/:vuosi/:organisaatioid/:sopimustyyppitunnus" []
        :auth [:view-tunnusluvut]
        :return [Kalusto]
        :path-params [vuosi :- Long
                      organisaatioid :- Long
                      sopimustyyppitunnus :- s/Str]
        :summary "Hae organisaation kalusto tietylle vuodella ja sopimustyypille."
        (ok (service/find-kalusto vuosi organisaatioid sopimustyyppitunnus)))
  (PUT* "/kalusto/:vuosi/:organisaatioid/:sopimustyyppitunnus" []
        :auth [:view-tunnusluvut]
        :return nil
        :path-params [vuosi :- Long
                      organisaatioid :- Long
                      sopimustyyppitunnus :- s/Str]
        :body [kalusto [Kalusto]]
        :summary "Tallenna organisaation kalusto tietylle vuodella ja sopimustyypille."
        (ok (service/save-kalusto! vuosi organisaatioid sopimustyyppitunnus kalusto)))

  ;; Lippuhinta
  (GET* "/lippuhinta/:vuosi/:organisaatioid/:sopimustyyppitunnus" []
        :auth [:view-tunnusluvut]
        :return [Lippuhinta]
        :path-params [vuosi :- Long
                      organisaatioid :- Long
                      sopimustyyppitunnus :- s/Str]
        :summary "Hae organisaation lippujen hinnat tietylle vuodella ja sopimustyypille."
        (ok (service/find-lippuhinta vuosi organisaatioid sopimustyyppitunnus)))
  (PUT* "/lippuhinta/:vuosi/:organisaatioid/:sopimustyyppitunnus" []
        :auth [:view-tunnusluvut]
        :return nil
        :path-params [vuosi :- Long
                      organisaatioid :- Long
                      sopimustyyppitunnus :- s/Str]
        :body [lippuhinnat [Lippuhinta]]
        :summary "Tallenna organisaation lippujen hinnat tietylle vuodella ja sopimustyypille."
        (ok (service/save-lippuhinta! vuosi organisaatioid sopimustyyppitunnus lippuhinnat)))

  ;; Lipputulo
  (GET* "/lipputulo/:vuosi/:organisaatioid/:sopimustyyppitunnus" []
        :auth [:view-tunnusluvut]
        :return [Lipputulo]
        :path-params [vuosi :- Long
                      organisaatioid :- Long
                      sopimustyyppitunnus :- s/Str]
        :summary "Hae organisaation lippujen hinnat tietylle vuodella ja sopimustyypille."
        (ok (service/find-lipputulo vuosi organisaatioid sopimustyyppitunnus)))
  (PUT* "/lipputulo/:vuosi/:organisaatioid/:sopimustyyppitunnus" []
        :auth [:view-tunnusluvut]
        :return nil
        :path-params [vuosi :- Long
                      organisaatioid :- Long
                      sopimustyyppitunnus :- s/Str]
        :body [lipputulot [Lipputulo]]
        :summary "Tallenna organisaation lippujen hinnat tietylle vuodella ja sopimustyypille."
        (ok (service/save-lipputulo! vuosi organisaatioid sopimustyyppitunnus lipputulot)))

  ;; Liikennöintikorvaus
  (GET* "/liikennointikorvaus/:vuosi/:organisaatioid/:sopimustyyppitunnus" []
        :auth [:view-tunnusluvut]
        :return [Liikennointikorvaus]
        :path-params [vuosi :- Long
                      organisaatioid :- Long
                      sopimustyyppitunnus :- s/Str]
        :summary "Hae organisaation liikennöintikorvaus tietylle vuodella ja sopimustyypille."
        (ok (service/find-lipputulo vuosi organisaatioid sopimustyyppitunnus)))
  (PUT* "/liikennointikorvaus/:vuosi/:organisaatioid/:sopimustyyppitunnus" []
        :auth [:view-tunnusluvut]
        :return nil
        :path-params [vuosi :- Long
                      organisaatioid :- Long
                      sopimustyyppitunnus :- s/Str]
        :body [korvaukset [Liikennointikorvaus]]
        :summary "Tallenna organisaation liikennöintikorvaus tietylle vuodella ja sopimustyypille."
        (ok (service/save-lipputulo! vuosi organisaatioid sopimustyyppitunnus korvaukset))))

