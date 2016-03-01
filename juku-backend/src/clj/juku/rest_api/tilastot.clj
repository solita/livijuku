(ns juku.rest-api.tilastot
  (:require [compojure.api.sweet :refer :all]
            [juku.service.tilastot :as service]
            [juku.schema.tunnusluku :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [common.core :as c]))

(defroutes*
  tilastot-routes

  (GET* "/tilastot/:tunnuslukuid/:organisaatiolajitunnus" []
        :auth [:view-tunnusluvut]
        :path-params [tunnuslukuid :- s/Str
                      organisaatiolajitunnus :- s/Str]

        :query-params [{vuosi :- Long nil}
                       {kuukausi :- Long nil}
                       {vyohykemaara :- Long nil}
                       {sopimustyyppitunnus :- s/Str nil}
                       {paastoluokkatunnus :- s/Str nil}
                       {viikonpaivaluokkatunnus :- s/Str nil}
                       {lipputuloluokkatunnus :- s/Str nil}
                       {lippuhintaluokkatunnus :- s/Str nil}
                       {kustannuslajitunnus :- s/Str nil}
                       group-by :- [s/Str]]

        :return [[s/Any]]
        :summary "Tunnuslukujen hakupalvelu."
        (ok (service/tunnusluku-tilasto (keyword tunnuslukuid) organisaatiolajitunnus
                                        (c/bindings->map vuosi
                                                         kuukausi
                                                         vyohykemaara
                                                         sopimustyyppitunnus
                                                         paastoluokkatunnus
                                                         viikonpaivaluokkatunnus
                                                         lipputuloluokkatunnus
                                                         lippuhintaluokkatunnus
                                                         kustannuslajitunnus)
                                        (map keyword group-by))))

  (GET* "/avustus/:organisaatiolajitunnus" []
        :auth [:view-tunnusluvut]
        :path-params [organisaatiolajitunnus :- s/Str]

        :return [[s/Num]]
        :summary "Valtion avustus ryhmiteltynä haettuun ja myönnettyyn avustukseen."
        (ok (service/select-avustus-ah0-group-by-vuosi (c/bindings->map organisaatiolajitunnus))))

  (GET* "/myonnettyavustus/:organisaatiolajitunnus" []
        :auth [:view-tunnusluvut]
        :path-params [organisaatiolajitunnus :- s/Str]

        :return [[s/Num]]
        :summary "Valtion avustus ryhmiteltynä haettuun ja myönnettyyn avustukseen."
        (ok (service/select-myonnettyavustus-ah0 (c/bindings->map organisaatiolajitunnus)))))

