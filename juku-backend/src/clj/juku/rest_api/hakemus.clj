(ns juku.rest-api.hakemus
  (:require [compojure.api.sweet :refer :all]
            [juku.service.hakemus :as service]
            [juku.service.hakemus-core :as core]
            [juku.schema.hakemus :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]))

(defroutes* hakemus-routes
      (GET* "/hakemussuunnitelmat/:vuosi/:hakemustyyppitunnus" []
            :auth [:kasittely-hakemus]
            :return [HakemusSuunnitelma]
            :path-params [vuosi :- s/Int, hakemustyyppitunnus :- s/Str]
            :summary "Hae hakemussuunnitelmat tietylle vuodella ja hakemustyypille."
            (ok (core/find-hakemussuunnitelmat vuosi hakemustyyppitunnus)))
      (GET* "/hakemus/:hakemusid" []
            :auth [:view-hakemus]
            :return Hakemus+
            :path-params [hakemusid :- Long]
            :summary "Hae hakemuksen perustiedot. Haettava hakemus yksilöidään hakemusid-polkuparametrilla."
            (ok (service/get-hakemus-by-id! hakemusid)))
      #_(POST* "/hakemus" []
             :auth [:modify-hakemuskausi]
             :return   s/Num
             :body     [hakemus NewHakemus]
             :summary  "Lisää yksittäinen hakemus."
             (ok (core/add-hakemus! hakemus)))
      (PUT* "/hakemus/:hakemusid/suunniteltuavustus" []
            :auth [:kasittely-hakemus]
            :return   nil
            :path-params [hakemusid :- Long]
            :body-params [suunniteltuavustus :- s/Num]
            :summary  "Päivittää hakemuksen myönnettävän avustusrahamäärän suunnitelmaan."
            (ok (core/save-hakemus-suunniteltuavustus! hakemusid suunniteltuavustus)))
      #_(PUT* "/hakemus/kasittelija" []
            :auth [:kasittely-hakemus]
            :return   nil
            :body-params [hakemusid :- Long, kasittelija :- s/Str]
            :summary  "Päivittää hakemuksen käsittelijän."
            (ok (core/save-hakemus-kasittelija! hakemusid kasittelija)))
      #_(PUT* "/hakemus/selite" []
            :auth [:modify-oma-hakemus]
            :return   nil
            :body-params [hakemusid :- Long, selite :- s/Str]
            :summary  "Päivittää hakemuksen selitteen."
            (ok (core/save-hakemus-selite! hakemusid selite)))
      (POST* "/hakemus/:hakemusid/laheta" []
             :auth [:allekirjoita-oma-hakemus]
             :audit []
             :return  nil
             :path-params     [hakemusid :- Long]
             :summary  "Hakija merkitsee hakemuksen lähetetyksi. Hakemus on tämän jälkeen vireillä."
             (ok (service/laheta-hakemus! hakemusid)))
      (POST* "/hakemus/:hakemusid/taydennyspyynto" []
             :auth [:kasittely-hakemus]
             :audit [:body-params]
             :return  nil
             :path-params     [hakemusid :- Long]
             :body-params [selite :- s/Str]
             :summary  "Käsittelijä lähettää hakijalle täydennyspyynnön. Hakemus on tämän jälkeen tilassa täydennettävää."
             (ok (service/taydennyspyynto! hakemusid selite)))
      (POST* "/hakemus/:hakemusid/laheta-taydennys" []
             :auth [:allekirjoita-oma-hakemus]
             :audit []
             :return  nil
             :path-params     [hakemusid :- Long]
             :summary  "Hakija lähettää täydennyksen hakemukseen. Hakemus on tämän jälkeen tilassa täydennetty."
             (ok (service/laheta-taydennys! hakemusid)))
      (POST* "/hakemus/:hakemusid/tarkasta" []
             :auth [:kasittely-hakemus]
             :audit []
             :return  nil
             :path-params     [hakemusid :- Long]
             :summary  "Käsittelijä merkitsee hakemuksen tarkastetuksi."
             (ok (service/tarkasta-hakemus! hakemusid)))
      (GET* "/hakemus/:hakemusid/pdf" []
            :auth [:view-hakemus]
            :path-params [hakemusid :- Long]
            :summary "Hae hakemuksen (hakemusid) hakemusasiakirja."
            (content-type (ok (service/find-hakemus-pdf hakemusid))
                          "application/pdf")))

