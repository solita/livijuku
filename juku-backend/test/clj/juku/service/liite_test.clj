(ns juku.service.liite-test
  (:require [midje.sweet :refer :all]
            [juku.service.hakemus :as h]
            [juku.service.liitteet :as l]
            [juku.service.test :as test])
  (:import (java.io ByteArrayInputStream)))

(let [hakemuskausi (test/next-hakemuskausi!)
      vuosi (:vuosi hakemuskausi)]

(fact "Uuden liitteen tallentaminen ja hakeminen"
  (let [organisaatioid 1M
        hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0"
                 :organisaatioid organisaatioid}

        id (h/add-hakemus! hakemus)
        liite {:hakemusid id :nimi "test" :contenttype "text/plain"}]

    (l/add-liite! liite (ByteArrayInputStream. (.getBytes "test")))
    (first (l/find-liitteet id)) => (assoc liite :liitenumero 1)))


(fact "Uuden liitteen tallentaminen - hakemusta ei ole olemassa"
  (let [liite {:hakemusid 1234234234 :nimi "test" :contenttype "text/plain"}]

    (l/add-liite! liite (ByteArrayInputStream. (.getBytes "test"))) =>
      (throws "Liitteen hakemusta (id = 1234234234) ei ole olemassa."))))