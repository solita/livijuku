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

        :return [[s/Any]]
        :summary "Valtion avustus ryhmiteltynä haettuun ja myönnettyyn avustukseen sekä vuoden perusteella."
        (ok (service/avustus-tilasto organisaatiolajitunnus)))

  (GET* "/avustus-details/:organisaatiolajitunnus" []
        :auth [:view-tunnusluvut]
        :path-params [organisaatiolajitunnus :- s/Str]

        :return [[s/Any]]
        :summary "Valtion avustus ryhmiteltynä organisaation ja vuoden perusteella."
        (ok (service/avustus-organisaatio-tilasto organisaatiolajitunnus)))

  (GET* "/avustus-asukas/:organisaatiolajitunnus" []
        :auth [:view-tunnusluvut]
        :path-params [organisaatiolajitunnus :- s/Str]

        :return [[s/Any]]
        :summary "Myönnetty valtion avustus asukastakohti ryhmiteltynä organisaation ja vuoden perusteella."
        (ok (service/avustus-asukastakohti-tilasto organisaatiolajitunnus)))

  (GET* "/omarahoitus-asukas/:organisaatiolajitunnus" []
        :auth [:view-tunnusluvut]
        :path-params [organisaatiolajitunnus :- s/Str]

        :return [[s/Any]]
        :summary "Organisaation omarahoitus asukastakohti ryhmiteltynä organisaation ja vuoden perusteella."
        (ok (service/omarahoitus-asukastakohti-tilasto organisaatiolajitunnus))))

