(ns juku.rest-api.kilpailutus
  (:require [compojure.api.sweet :refer :all]
            [juku.service.kilpailutus :as service]
            [juku.schema.kilpailutus :refer :all]
            [ring.util.http-response :refer :all]
            [ring.util.io :as ring-io]
            [schema.core :as s]
            [juku.schema.common :as sc]
            [common.core :as c]))

(defroutes*
  kilpailutus-routes

  (GET* "/kilpailutus/:kilpailutusid" []
        :return Kilpailutus
        :path-params [kilpailutusid :- Long]
        :summary "Hae kilapilutuksen tiedot. Haettava kilpailutus yksilöidään kilpailutusid-polkuparametrilla."
        (ok (service/get-kilpailutus! kilpailutusid)))

  (PUT* "/kilpailutus/:kilpailutusid" []
        :audit []
        :return nil
        :path-params [kilpailutusid :- Long]
        :body [kilpailutus EditKilpailutus]
        :summary "Päivittää kilpailutuksen tiedot."
        (ok (service/edit-kilpailutus! kilpailutusid kilpailutus)))

  (POST* "/kilpailutus" []
         :audit []
         :return s/Num
         :body [kilpailutus EditKilpailutus]
         :summary "Lisää uuden kilpailutuksen järjestelmään."
         (ok (service/add-kilpailutus! kilpailutus)))

  (DELETE* "/kilpailutus/:kilpailutusid" []
           :audit []
           :return nil
           :path-params [kilpailutusid :- Long]
           :summary "Poistaa kilpailutuksen järjestelmäästä."
          (ok (service/delete-kilpailutus! kilpailutusid)))

  (GET* "/kilpailutukset" []
        :query-params [{organisaatioid :- [Long] nil}]

        :summary "Hae kilpailutuksia annettujen rajoitusehtojen mukaisesti."
        (ok (service/find-kilpailutukset (c/bindings->map organisaatioid))))

  (PUT* "/kilpailutukset/import" []
        :return s/Num
        :body [csv [[s/Str]]]
        :summary "Lataa tunnusluvut csv-muodossa."
        (ok (service/import-kilpailutukset! csv))))

