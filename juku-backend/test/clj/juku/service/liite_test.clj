(ns juku.service.liite-test
  (:require [midje.sweet :refer :all]
            [juku.service.hakemus-core :as hc]
            [juku.service.hakemus :as h]
            [juku.service.asiahallinta-mock :as asha]
            [juku.service.liitteet :as l]
            [juku.service.test :as test]))

(def hakemuskausi (test/next-avattu-empty-hakemuskausi!))
(def vuosi (:vuosi hakemuskausi))
(test/set-hakuaika-today vuosi "AH0")
(def hsl-hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid 1M})

(test/with-user "juku_hakija" ["juku_hakija"]
  (fact "Uuden liitteen tallentaminen ja hakeminen"
    (let [id (hc/add-hakemus! hsl-hakemus)
          liite {:hakemusid id :nimi "test" :contenttype "text/plain"}]

      (l/add-liite! liite (test/inputstream-from "test"))
      (count (l/find-liitteet id)) => 1
      (first (l/find-liitteet id)) => (assoc liite :liitenumero 1 :bytesize 4M)))

  (fact "Liiteen poistaminen"
    (let [id (hc/add-hakemus! hsl-hakemus)
          liite {:hakemusid id :nimi "test" :contenttype "text/plain"}]

      (l/add-liite! liite (test/inputstream-from "test"))
      (l/add-liite! liite (test/inputstream-from "test"))
      (count (l/find-liitteet id)) => 2
      (every? (partial = liite) (l/find-liitteet id))

      (l/delete-liite! id 2)
      (count (l/find-liitteet id)) => 1
      (first (l/find-liitteet id)) => (assoc liite :liitenumero 1 :bytesize 4M)))

  (fact "Liitteen tallentaminen - hakija ei omistaja"
    (let [id (hc/add-hakemus! {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid 2M})
          liite {:hakemusid id :nimi "test" :contenttype "text/plain"}]

      (l/add-liite! liite (test/inputstream-from "test")) =>
        (throws (str "Käyttäjä juku_hakija ei ole hakemuksen: " id " omistaja."))))

  (fact "Liitteen tallentaminen - hakemus lähetetty"
    (let [id (hc/add-hakemus! hsl-hakemus)
          liite {:hakemusid id :nimi "test" :contenttype "text/plain"}]

      (asha/with-asha (h/laheta-hakemus! id))
      (l/add-liite! liite (test/inputstream-from "test")) =>
        (throws (str "Hakemusta " id " ei voi muokata, koska se ei ole enää keskeneräinen. Hakemus on lähetetty käsiteltäväksi."))))

  (fact "Liitteen tallentaminen - hakemusta ei ole olemassa"
    (l/add-liite! {:hakemusid 1234234234 :nimi "test" :contenttype "text/plain"}
                    (test/inputstream-from "test")) =>
        (throws "Hakemusta 1234234234 ei ole olemassa.")))
