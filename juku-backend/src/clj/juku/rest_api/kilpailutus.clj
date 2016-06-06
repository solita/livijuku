(ns juku.rest-api.kilpailutus
  (:require [compojure.api.sweet :refer :all]
            [juku.service.kilpailutus :as service]
            [juku.schema.kilpailutus :refer :all]
            [ring.util.http-response :refer :all]
            [ring.util.io :as ring-io]
            [schema.core :as s]
            [juku.schema.common :as sc]
            [common.core :as c]))

(defroutes
  kilpailutus-public-routes

  (GET "/kilpailutus/sopimusmallit" []
    :auth [:view-kilpailutus]
    :return [sc/Luokka]
    :summary "Hae kilpailutusten sopimusmallit."
    (ok (service/find-sopimusmallit)))

  (GET "/kilpailutus/:kilpailutusid" []
    :auth [:view-kilpailutus]
    :return Kilpailutus
    :path-params [kilpailutusid :- Long]
    :summary "Hae kilpailutuksen tiedot. Haettava kilpailutus yksilöidään kilpailutusid-polkuparametrilla."
    (ok (service/get-kilpailutus! kilpailutusid)))

  (GET "/kilpailutukset" []
    :auth [:view-kilpailutus]
    :query-params [{organisaatioid :- [Long] nil}]

    :summary "Hae kilpailutuksia annettujen rajoitusehtojen mukaisesti."
    (ok (service/find-kilpailutukset (c/bindings->map organisaatioid)))))

(defroutes
  kilpailutus-routes

  (PUT "/kilpailutus/:kilpailutusid" []
        :return nil
        :path-params [kilpailutusid :- Long]
        :body [kilpailutus EditKilpailutus]
        :summary "Päivittää kilpailutuksen tiedot."
        (ok (service/edit-kilpailutus! kilpailutusid kilpailutus)))

  (POST "/kilpailutus" []
         :return s/Num
         :body [kilpailutus EditKilpailutus]
         :summary "Lisää uuden kilpailutuksen järjestelmään."
         (ok (service/add-kilpailutus! kilpailutus)))

  (DELETE "/kilpailutus/:kilpailutusid" []
           :return nil
           :path-params [kilpailutusid :- Long]
           :summary "Poistaa kilpailutuksen järjestelmäästä."
          (ok (service/delete-kilpailutus! kilpailutusid)))

  (PUT "/kilpailutukset/import" []
    :audit [:modify-kaikki-kilpailutukset]
    :return nil
    :body [csv [[s/Str]]]
    :summary "Lataa tunnusluvut csv-muodossa."
    (ok (service/import-kilpailutukset! csv)))

  (GET "/kilpailutukset/csv*" []
    :auth [:view-kilpailutus]
    :summary "Lataa kaikkien kilpailutuksien tiedot csv-muodossa."
    (content-type (ok (ring-io/piped-input-stream service/export-kilpailutukset-csv))
                  "text/csv")))

