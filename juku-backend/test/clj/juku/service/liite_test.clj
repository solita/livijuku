(ns juku.service.liite-test
  (:require [midje.sweet :refer :all]
            [clj-time.core :as t]
            [juku.service.hakemus :as h]
            [juku.service.liitteet :as l])
  (:import (java.io ByteArrayInputStream)))

(fact "Uuden liitteen tallentaminen ja hakeminen"
  (let [organisaatioid 1M
        hakemus {:vuosi 2015 :hakemustyyppitunnus "AH0"
                 :organisaatioid organisaatioid
                 :hakuaika {:alkupvm (t/local-date 2014 6 1)
                            :loppupvm (t/local-date 2014 12 1)}}

        id (h/add-hakemus! hakemus)
        liite {:hakemusid id :nimi "test" :contenttype "text/plain"}]

    (l/add-liite! liite (ByteArrayInputStream. (.getBytes "test")))
    (first (l/find-liitteet id)) => (assoc liite :liitenumero 1)))


(fact "Uuden liitteen tallentaminen - hakemusta ei ole olemassa"
  (let [organisaatioid 1M
        hakemus {:vuosi 2015 :hakemustyyppitunnus "AH0"
                 :organisaatioid organisaatioid
                 :hakuaika {:alkupvm (t/local-date 2014 6 1)
                            :loppupvm (t/local-date 2014 12 1)}}

        liite {:hakemusid 1234234234 :nimi "test" :contenttype "text/plain"}]

    (l/add-liite! liite (ByteArrayInputStream. (.getBytes "test"))) =>
      (throws "Liitteen hakemusta (id = 1234234234) ei ole olemassa.")))