(ns juku.rest-api.liitteet
  (:require [compojure.api.sweet :refer :all]
            [compojure.api.legacy :refer :all]
            [juku.service.liitteet :as service]
            [juku.rest-api.response :as response]
            [juku.schema.liitteet :refer :all]
            [ring.util.http-response :refer :all]
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
          (if-let [liite (service/find-liite-sisalto hakemusid liitenumero)]
            (response/content-disposition-inline (:nimi liite) (content-type (ok (:sisalto liite)) (:contenttype liite)))
            (not-found (str "Hakemuksella " hakemusid " ei ole liitettä: " liitenumero))))

    (GET* "/hakemus/:hakemusid/liite/:liitenumero/*" []
          :path-params [hakemusid :- Long, liitenumero :- Long]
          :summary "Lataa liitteen sisältö - liitenumeron jälkeen annetaan haluttu tiedostonimi selaimelle, joka ei tue rfc6266 ja rfc5987."
          (if-let [liite (service/find-liite-sisalto hakemusid liitenumero)]
            (content-type (ok (:sisalto liite)) (:contenttype liite))
            (not-found (str "Hakemuksella " hakemusid " ei ole liitettä: " liitenumero))))

    (POST "/hakemus/:hakemusid/liite"
            [hakemusid :as {{{tempfile :tempfile filename :filename contenttype :content-type} :liite} :params}]
            (ok (service/add-liite! {:hakemusid hakemusid :nimi filename :contenttype contenttype} (io/input-stream tempfile))))

    (PUT* "/hakemus/:hakemusid/liite/:liitenumero" []
             :return nil
             :path-params [hakemusid :- Long, liitenumero :- Long]
             :body-params     [nimi :- s/Str]
             :summary "Päivitä liitteen nimi"
             (ok (service/update-liite-nimi! hakemusid liitenumero nimi)))

    (DELETE* "/hakemus/:hakemusid/liite/:liitenumero" []
         :return nil
         :path-params [hakemusid :- Long, liitenumero :- Long]
         :summary "Poista hakemuksen liite"
         (ok (service/delete-liite hakemusid liitenumero))))
