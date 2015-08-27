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
            :auth [:view-kaikki-hakemukset :view-oma-hakemus]
            :return Hakemus+
            :path-params [hakemusid :- Long]
            :summary "Hae hakemuksen perustiedot. Haettava hakemus yksilöidään hakemusid-polkuparametrilla."
            (ok (service/get-hakemus-by-id! hakemusid)))
      (POST* "/hakemus" []
             :auth [:modify-hakemuskausi]
             :return   s/Num
             :body     [hakemus NewHakemus]
             :summary  "Lisää yksittäinen hakemus."
             (ok (core/add-hakemus! hakemus)))
      (PUT* "/hakemus/suunniteltuavustus" []
            :auth [:kasittely-hakemus]
            :return   nil
            :body-params [hakemusid :- Long, suunniteltuavustus :- s/Num]
            :summary  "Päivittää hakemuksen myönnettävän avustusrahamäärän suunnitelmaan."
            (ok (core/save-hakemus-suunniteltuavustus! hakemusid suunniteltuavustus)))
      (PUT* "/hakemus/kasittelija" []
            :auth [:kasittely-hakemus]
            :return   nil
            :body-params [hakemusid :- Long, kasittelija :- s/Str]
            :summary  "Päivittää hakemuksen käsittelijän."
            (ok (core/save-hakemus-kasittelija! hakemusid kasittelija)))
      (PUT* "/hakemus/selite" []
            :auth [:modify-oma-hakemus]
            :return   nil
            :body-params [hakemusid :- Long, selite :- s/Str]
            :summary  "Päivittää hakemuksen selitteen."
            (ok (core/save-hakemus-selite! hakemusid selite)))
      (POST* "/laheta-hakemus" []
             :auth [:allekirjoita-oma-hakemus]
             :return  nil
             :body-params     [hakemusid :- Long]
             :summary  "Hakija merkitsee hakemuksen lähetetyksi. Hakemus on tämän jälkeen vireillä."
             (ok (service/laheta-hakemus! hakemusid)))
      (POST* "/taydennyspyynto" []
             :auth [:kasittely-hakemus]
             :return  nil
             :body [taydennyspyynto NewTaydennyspyynto]
             :summary  "Käsittelijä lähettää hakijalle täydennyspyynnön. Hakemus on tämän jälkeen tilassa täydennettävää."
             (ok (service/taydennyspyynto! (:hakemusid taydennyspyynto) (:selite taydennyspyynto))))
      (POST* "/laheta-taydennys" []
             :auth [:allekirjoita-oma-hakemus]
             :return  nil
             :body-params     [hakemusid :- Long]
             :summary  "Hakija lähettää täydennyksen hakemukseen. Hakemus on tämän jälkeen tilassa täydennetty."
             (ok (service/laheta-taydennys! hakemusid)))
      (POST* "/tarkasta-hakemus" []
             :auth [:kasittely-hakemus]
             :return  nil
             :body-params     [hakemusid :- Long]
             :summary  "Käsittelijä merkitsee hakemuksen tarkastetuksi."
             (ok (service/tarkasta-hakemus! hakemusid)))
      (GET* "/hakemus/:hakemusid/pdf" []
            :auth [:view-kaikki-hakemukset :view-oma-hakemus]
            :path-params [hakemusid :- Long]
            :summary "Hae hakemuksen (hakemusid) hakemusasiakirja."
            (content-type (ok (service/find-hakemus-pdf hakemusid))
                          "application/pdf")))

