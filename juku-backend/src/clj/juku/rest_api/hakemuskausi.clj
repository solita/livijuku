(ns juku.rest-api.hakemuskausi
  (:require [compojure.api.sweet :refer :all]
            [compojure.api.legacy :refer :all]
            [juku.service.hakemuskausi :as service]
            [juku.schema.hakemus :refer :all]
            [ring.util.http-response :refer :all]
            [ring.swagger.schema :refer [describe]]
            [schema.core :as s]
            [clojure.java.io :as io]))

(defroutes* hakemuskausi-routes
      (POST* "/hakemuskausi" []
             :return   nil
             :body-params     [vuosi :- s/Int]
             :summary  "Avaa uusi hakemuskausi."
             (ok (service/avaa-hakemuskausi! vuosi)))

      (PUT "/hakemuskausi/:vuosi/hakuohje"
            [vuosi :as {{{tempfile :tempfile filename :filename contenttype :content-type} :hakuohje} :params}]
            (ok (service/save-hakuohje vuosi filename contenttype (io/input-stream tempfile)))))

