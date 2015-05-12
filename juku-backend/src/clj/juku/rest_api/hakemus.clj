(ns juku.rest-api.hakemus
  (:require [compojure.api.sweet :refer :all]
            [juku.service.hakemus :as service]
            [juku.schema.hakemus :refer :all]
            [ring.util.http-response :refer :all]
            [ring.swagger.schema :refer [describe]]
            [schema.core :as s]))

(defroutes* hakemus-routes
      (GET* "/hakemussuunnitelmat/:vuosi/:hakemustyyppitunnus" []
            :return [HakemusSuunnitelma]
            :path-params [vuosi :- s/Int, hakemustyyppitunnus :- s/Str]
            :summary "Hae hakemussuunnitelmat tietylle vuodella ja hakemustyypille."
            (ok (service/find-hakemussuunnitelmat vuosi hakemustyyppitunnus)))
      (GET* "/hakemukset/hakija" []
            :return [Hakemuskausi]
            :summary "Hae sisäänkirjautuneen käyttäjän hakemukset hakemuskausittain (vuosittain) ryhmitettynä."
            (ok (service/find-kayttajan-hakemukset-vuosittain)))
      (GET* "/hakemus/:hakemusid" []
            :return Hakemus+
            :path-params [hakemusid :- Long]
            :summary "Hae hakemuksen perustiedot. Haettava hakemus yksilöidään hakemusid-polkuparametrilla."
            (ok (service/get-hakemus-by-id! hakemusid)))
      (POST* "/hakemus" []
             :return   s/Num
             :body     [hakemus NewHakemus]
             :summary  "Lisää yksittäinen hakemus."
             (ok (service/add-hakemus! hakemus)))
      (PUT* "/hakemus/suunniteltuavustus" []
            :return   s/Num
            :body-params [hakemusid :- Long, suunniteltuavustus :- s/Num]
            :summary  "Päivittää hakemuksen myönnettävän avustusrahamäärän suunnitelmaan."
            (ok (service/save-hakemus-suunniteltuavustus! hakemusid suunniteltuavustus)))
      (PUT* "/hakemus/kasittelija" []
            :return   s/Num
            :body-params [hakemusid :- Long, kasittelija :- s/Str]
            :summary  "Päivittää hakemuksen käsittelijän."
            (ok (service/save-hakemus-kasittelija! hakemusid kasittelija)))
      (PUT* "/hakemus/selite" []
            :return   s/Num
            :body-params [hakemusid :- Long, selite :- s/Str]
            :summary  "Päivittää hakemuksen selitteen."
            (ok (service/save-hakemus-selite! hakemusid selite)))
      (POST* "/laheta-hakemus" []
             :return  nil
             :body-params     [hakemusid :- Long]
             :summary  "Hakija merkitsee hakemuksen lähetetyksi. Hakemus on tämän jälkeen vireillä."
             (ok (service/laheta-hakemus! hakemusid)))
      (POST* "/taydennyspyynto" []
             :return  nil
             :body-params     [hakemusid :- Long]
             :summary  "Käsittelijä lähettää hakijalle täydennyspyynnön. Hakemus on tämän jälkeen tilassa täydennettävää."
             (ok (service/taydennyspyynto! hakemusid)))
      (POST* "/laheta-taydennys" []
             :return  nil
             :body-params     [hakemusid :- Long]
             :summary  "Hakija lähettää täydennyksen hakemukseen. Hakemus on tämän jälkeen tilassa täydennetty."
             (ok (service/laheta-taydennys! hakemusid)))
      (POST* "/tarkasta-hakemus" []
             :return  nil
             :body-params     [hakemusid :- Long]
             :summary  "Käsittelijä merkitsee hakemuksen tarkastetuksi."
             (ok (service/tarkasta-hakemus! hakemusid)))
      (GET* "/hakemus/:hakemusid/pdf" []
            :path-params [hakemusid :- Long]
            :summary "Hae hakemuksen (hakemusid) hakemusasiakirja."
            (content-type (ok (service/hakemus-pdf hakemusid))
                          "application/pdf")))

