(ns juku.rest-api.hakemuskausi
  (:require [compojure.api.sweet :refer :all]
            [compojure.api.legacy :refer :all]
            [juku.service.hakemuskausi :as service]
            [juku.schema.hakemuskausi :refer :all]
            [ring.util.http-response :refer :all]
            [compojure.api.upload :as upload]
            [schema.core :as s]
            [clojure.java.io :as io]))

(defn hakuohje-response [vuosi hakuohje]
  (if hakuohje
    (content-type (ok (:sisalto hakuohje)) (:contenttype hakuohje))
    (not-found (str "Hakemuskaudella " vuosi " ei ole ohjetta."))))

(defroutes* hakemuskausi-routes
      (GET* "/hakemuskaudet" []
            :auth [:view-hakemus]
            :return [Hakemuskausi+Hakemukset+Kasittely]
            :summary "Hae kaikki hakemuskaudet ja niiden hakemukset."
            (ok (service/find-hakemuskaudet+hakemukset)))

      (GET* "/hakemuskaudet/yhteenveto" []
            :auth [:modify-hakemuskausi]
            :return [Hakemuskausi+Summary]
            :summary "Hae kaikki hakemuskaudet ja yhteenvedon jokaisen hakemuskauden hakemustyypeistä, tiloista ja hakuajoista."
            (ok (service/find-hakemuskaudet+summary)))

      (GET* "/hakemuskaudet/omat" []
            :auth [:view-oma-hakemus]
            :return [Hakemuskausi+Hakemukset]
            :summary "Hae sisäänkirjautuneen käyttäjän omat hakemukset hakemuskausittain ryhmitettynä."
            (ok (service/find-kayttajan-hakemuskaudet+hakemukset)))

      (POST* "/hakemuskausi/:vuosi" []
             :auth [:modify-hakemuskausi]
             :audit []
             :return   nil
             :path-params     [vuosi :- s/Int]
             :summary  "Avaa uusi hakemuskausi."
             (ok (service/avaa-hakemuskausi! vuosi)))

      (GET* "/maararaha/:vuosi/:organisaatiolajitunnus" []
            :auth [:modify-hakemuskausi]
            :return (s/maybe Maararaha)
            :path-params [vuosi :- s/Int, organisaatiolajitunnus :- s/Str]
            :summary "Hae määräraha tietylle vuodella ja organisaatiolajille."
            (ok (service/find-maararaha vuosi organisaatiolajitunnus)))

      (GET* "/hakemuskausi/:vuosi/hakuohje" []
            :auth [:view-hakemus]
            :path-params [vuosi :- s/Int]
            :summary "Lataa hakuohjeen sisältö."
            (hakuohje-response vuosi (service/find-hakuohje-sisalto vuosi)))

      (GET* "/hakemuskausi/:vuosi/elyhakuohje" []
            :auth [:view-hakemus]
            :path-params [vuosi :- s/Int]
            :summary "Lataa ely-hakuohjeen sisältö."
            (hakuohje-response vuosi (service/find-ely-hakuohje-sisalto vuosi)))

      (PUT* "/hakemuskausi/:vuosi/hakuajat" []
            :auth [:modify-hakemuskausi]
            :audit [:body-params]
            :return   nil
            :path-params [vuosi :- s/Int]
            :body     [hakuajat [Hakuaika+]]
            :summary  "Päivittää hakemuskauden hakuajat."
            (ok (service/save-hakemuskauden-hakuajat! vuosi hakuajat)))

      (PUT* "/maararaha/:vuosi/:organisaatiolajitunnus" []
            :auth [:modify-hakemuskausi]
            :audit [:body-params]
            :return   nil
            :path-params [vuosi :- s/Int, organisaatiolajitunnus :- s/Str]
            :body     [maararaha Maararaha]
            :summary  "Päivittää tai lisää määrärahan tietylle vuodella ja organisaatiolajille."
            (ok (service/save-maararaha! (assoc maararaha :vuosi vuosi :organisaatiolajitunnus organisaatiolajitunnus))))

      (PUT* "/hakemuskausi/:vuosi/hakuohje" []
             :auth [:modify-hakemuskausi]
             :audit []
             :path-params [vuosi :- s/Int]
             :multipart-params [hakuohje :- upload/TempFileUpload]
             :summary "Päivittää hakemuskauden hakuohjeen."
             (ok (service/save-hakuohje vuosi
                                        (:filename hakuohje)
                                        (:content-type hakuohje)
                                        (io/input-stream (:tempfile hakuohje)))))

      ;; this end point is for legacy UAs which does not support put requests
      (POST* "/hakemuskausi/:vuosi/hakuohje" []
             :auth [:modify-hakemuskausi]
             :audit []
             :path-params [vuosi :- s/Int]
             :multipart-params [hakuohje :- upload/TempFileUpload
                                Filename :- s/Any
                                Upload :- s/Any]
             :summary "Päivittää hakemuskauden hakuohjeen. Tämä palvelu on tarkoitettu ie9 flash tiedoston lataukselle."
             (ok (service/save-hakuohje vuosi
                                        (:filename hakuohje)
                                        (:content-type hakuohje)
                                        (io/input-stream (:tempfile hakuohje)))))

      (PUT* "/hakemuskausi/:vuosi/elyhakuohje" []
            :auth [:modify-hakemuskausi]
            :audit []
            :path-params [vuosi :- s/Int]
            :multipart-params [hakuohje :- upload/TempFileUpload]
            :summary "Päivittää hakemuskauden ely-hakuohjeen."
            (ok (service/save-ely-hakuohje vuosi
                                       (:filename hakuohje)
                                       (:content-type hakuohje)
                                       (io/input-stream (:tempfile hakuohje)))))

      ;; this end point is for legacy UAs which does not support put requests
      (POST* "/hakemuskausi/:vuosi/elyhakuohje" []
             :auth [:modify-hakemuskausi]
             :audit []
             :path-params [vuosi :- s/Int]
             :multipart-params [hakuohje :- upload/TempFileUpload
                                Filename :- s/Any
                                Upload :- s/Any]
             :summary "Päivittää hakemuskauden ely-hakuohjeen. Tämä palvelu on tarkoitettu ie9 flash tiedoston lataukselle."
             (ok (service/save-ely-hakuohje vuosi
                                        (:filename hakuohje)
                                        (:content-type hakuohje)
                                        (io/input-stream (:tempfile hakuohje)))))

      (POST* "/hakemuskausi/:vuosi/sulje" []
             :auth [:modify-hakemuskausi]
             :audit []
             :return   nil
             :path-params     [vuosi :- s/Int]
             :summary  "Sulje olemassaoleva hakemuskausi."
             (ok (service/sulje-hakemuskausi! vuosi))))

