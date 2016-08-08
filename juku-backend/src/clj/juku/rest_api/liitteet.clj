(ns juku.rest-api.liitteet
  (:require [compojure.api.sweet :refer :all]
            [compojure.api.upload :as upload]
            [juku.service.liitteet :as service]
            [juku.rest-api.response :as response]
            [juku.schema.liitteet :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [clojure.java.io :as io]))

(defroutes liitteet-routes
    (GET "/hakemus/:hakemusid/liitteet" []
          :auth [:view-hakemus]
          :return [Liite+Size]
          :path-params [hakemusid :- Long]
          :summary "Hae hakemuksen liitteet."
          (ok (service/find-liitteet hakemusid)))

    (GET "/hakemus/:hakemusid/liite/:liitenumero" []
          :auth [:view-hakemus]
          :path-params [hakemusid :- Long, liitenumero :- Long]
          :summary "Lataa liitteen sisältö."
          (if-let [liite (service/find-liite-sisalto hakemusid liitenumero)]
            (response/content-disposition-inline (:nimi liite) (content-type (ok (:sisalto liite)) (:contenttype liite)))
            (not-found (str "Hakemuksella " hakemusid " ei ole liitettä: " liitenumero))))

    (GET "/hakemus/:hakemusid/liite/:liitenumero/*" []
          :auth [:view-hakemus]
          :path-params [hakemusid :- Long, liitenumero :- Long]
          :summary "Lataa liitteen sisältö - liitenumeron jälkeen annetaan haluttu tiedostonimi selaimelle, joka ei tue rfc6266 ja rfc5987."
          (if-let [liite (service/find-liite-sisalto hakemusid liitenumero)]
            (content-type (ok (:sisalto liite)) (:contenttype liite))
            (not-found (str "Hakemuksella " hakemusid " ei ole liitettä: " liitenumero))))

    (POST "/hakemus/:hakemusid/liite" []
           :auth [:modify-oma-hakemus]
           :audit []
           :path-params [hakemusid :- Long]
           :multipart-params [liite :- upload/TempFileUpload
                              {Filename :- s/Any nil}
                              {Upload :- s/Any nil}]
           :summary "Tallenna uusi hakemusliite juku-järjestelmään."
           (ok (service/add-liite-from-file! {:hakemusid hakemusid
                                              :nimi (:filename liite)
                                              :contenttype (:content-type liite)}
                                             (:tempfile liite))))


    (PUT "/hakemus/:hakemusid/liite/:liitenumero" []
          :auth [:modify-oma-hakemus]
          :audit [:body-params]
          :return nil
          :path-params [hakemusid :- Long, liitenumero :- Long]
          :body-params     [nimi :- s/Str]
          :summary "Päivitä liitteen nimi"
          (ok (service/update-liite-nimi! hakemusid liitenumero nimi)))

    (DELETE "/hakemus/:hakemusid/liite/:liitenumero" []
         :auth [:modify-oma-hakemus]
         :audit []
         :return nil
         :path-params [hakemusid :- Long, liitenumero :- Long]
         :summary "Poista hakemuksen liite"
         (ok (service/delete-liite! hakemusid liitenumero))))
