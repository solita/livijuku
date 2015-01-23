(ns juku.rest-api.liitteet
  (:require [compojure.api.sweet :refer :all]
            [compojure.api.legacy :refer :all]
            [juku.service.liitteet :as service]
            [juku.schema.liitteet :refer :all]
            [ring.util.http-response :refer :all]
            [ring.swagger.schema :refer [describe]]
            [schema.core :as s]
            [clojure.java.io :as io]))

(defroutes* liitteet-routes
    (GET* "/hakemus/:hakemusid/liitteet" []
          :return [Liite]
          :path-params [hakemusid :- Long]
          :summary "Hae hakemuksen liitteet."
          (ok (service/find-liitteet hakemusid)))

    (GET* "/hakemus/:hakemusid/liite/:liitenumero" []
          :path-params [hakemusid :- Long, liitenumero :- Long]
          :summary "Lataa liitteen sisältö."
          (ok (:sisalto (service/find-liite-sisalto hakemusid liitenumero))))

    (POST "/hakemus/:hakemusid/liite"
            [hakemusid :as {{{tempfile :tempfile filename :filename contenttype :content-type} :liite} :params}]
            (ok (service/add-liite! {:hakemusid hakemusid :nimi filename :contenttype contenttype} (io/input-stream tempfile))))

    (DELETE* "/hakemus/:hakemusid/liite/:liitenumero" []
         :return nil
         :path-params [hakemusid :- Long, liitenumero :- Long]
         :summary "Poista hakemuksen liite"
         (ok (service/delete-liite hakemusid liitenumero))))
