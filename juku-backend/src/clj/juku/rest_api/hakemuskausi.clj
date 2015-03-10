(ns juku.rest-api.hakemuskausi
  (:require [compojure.api.sweet :refer :all]
            [compojure.api.legacy :refer :all]
            [juku.service.hakemuskausi :as service]
            [juku.schema.hakemuskausi :refer :all]
            [ring.util.http-response :refer :all]
            [ring.swagger.schema :refer [describe]]
            [schema.core :as s]
            [clojure.java.io :as io]))

(defroutes* hakemuskausi-routes
      (GET* "/hakemuskaudet" []
            :return [Hakemuskausi+]
            :summary "Hae kaikki hakemuskaudet ja niiden hakemukset."
            (ok (service/find-hakemuskaudet+hakemukset)))

      #_(GET* "/hakemuskaudet/yhteenveto" []
            :return [Hakemuskausi+]
            :summary "Hae kaikki hakemuskaudet ja yhteenvedon jokaisen hakemuskauden tiloista ja hakuajoista."
            (ok (service/find-hakemuskaudet+summary)))

      (POST* "/hakemuskausi" []
             :return   nil
             :body-params     [vuosi :- s/Int]
             :summary  "Avaa uusi hakemuskausi."
             (ok (service/avaa-hakemuskausi! vuosi)))

      (GET* "/maararaha/:vuosi/:organisaatiolajitunnus" []
            :return (s/maybe Maararaha)
            :path-params [vuosi :- s/Int, organisaatiolajitunnus :- s/Str]
            :summary "Hae määräraha tietylle vuodella ja organisaatiolajille."
            (ok (service/find-maararaha vuosi organisaatiolajitunnus)))

      (GET* "/hakemuskausi/:vuosi/hakuohje" []
            :path-params [vuosi :- s/Int]
            :summary "Lataa hakuohjeen sisältö."
            (if-let [hakuohje (service/find-hakuohje-sisalto vuosi)]
              (content-type (ok (:sisalto hakuohje)) (:contenttype hakuohje))
              (not-found (str "Hakemuskaudella " vuosi " ei ole ohjetta."))))

      (PUT* "/maararaha/:vuosi/:organisaatiolajitunnus" []
            :return   nil
            :path-params [vuosi :- s/Int, organisaatiolajitunnus :- s/Str]
            :body     [maararaha Maararaha]
            :summary  "Päivittää tai lisää määrärahan tietylle vuodella ja organisaatiolajille."
            (ok (service/save-maararaha! (assoc maararaha :vuosi vuosi :organisaatiolajitunnus organisaatiolajitunnus))))

      (PUT "/hakemuskausi/:vuosi/hakuohje"
            [vuosi :as {{{tempfile :tempfile filename :filename content-type :content-type} :hakuohje} :params}]
            (ok (service/save-hakuohje (Integer/parseInt vuosi) filename content-type (io/input-stream tempfile)))))

