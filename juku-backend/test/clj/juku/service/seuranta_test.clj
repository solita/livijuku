(ns juku.service.seuranta-test
  (:require [midje.sweet :refer :all]
            [clojure.string :as str]
            [common.collection :as c]
            [juku.service.hakemus-core :as hc]
            [juku.service.hakemus :as h]
            [juku.service.seuranta :as s]
            [juku.service.avustuskohde :as ak]
            [juku.service.test :as test]))

(defn find-by-id [id] (fn [m] (= (:id m) id)))

(def hakemuskausi (test/next-avattu-empty-hakemuskausi!))
(def vuosi (:vuosi hakemuskausi))
(def hsl-ah0-hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid 1M})

(defn liikennesuorite
  ([hakemusid numero] (liikennesuorite hakemusid "PSA" numero))
  ([hakemusid liikennetyyppitunnus numero] {
   :hakemusid            hakemusid,
   :liikennetyyppitunnus liikennetyyppitunnus,
   :numero               numero,

   :suoritetyyppitunnus  "LS",
   :nimi                 "testilinja",
   :linjaautot           1M,
   :taksit               1M,
   :ajokilometrit        1M,
   :matkustajamaara      1M,
   :asiakastulo          1M,
   :nettohinta           1M,
   :bruttohinta          1M
}))

; -- Liikennesuoritteiden testit --

(facts
  "Liikennesuoritteiden testit"
  (test/with-user
    "juku_hakija" ["juku_hakija"]
    (fact "Liikennesuoritteen tallentaminen ja haku"
      (let [id (hc/add-hakemus! hsl-ah0-hakemus)
            liikennesuorite (liikennesuorite id 1M) ]

        (s/save-liikennesuoritteet! id [liikennesuorite])

        (s/find-hakemus-liikennesuoritteet id) => [liikennesuorite]))

    (fact "Usean liikennesuoritteen tallentaminen ja haku"
      (let [id (hc/add-hakemus! hsl-ah0-hakemus)
            l1 (liikennesuorite id 1M)
            l2 (liikennesuorite id 2M)]

        (s/save-liikennesuoritteet! id [l1 l2])

        (s/find-hakemus-liikennesuoritteet id) => [l1 l2]))

    (fact "Liikennesuoritteen päivittäminen ja haku"
      (let [id (hc/add-hakemus! hsl-ah0-hakemus)
            l1 (liikennesuorite id 1M)
            l2 (liikennesuorite id 2M)]

        (s/save-liikennesuoritteet! id [l1 l2])
        (s/save-liikennesuoritteet! id [l1])

        (s/find-hakemus-liikennesuoritteet id) => [l1]))))

(facts
  "Liikennesuoritteiden testit - virhetilanteet"
  (test/with-user
    "juku_hakija" ["juku_hakija"]
    (fact "Liikennesuoritteen tallentaminen - liikennetyyppiä ei ole olemassa"
      (let [id (hc/add-hakemus! hsl-ah0-hakemus)
            liikennesuorite (liikennesuorite id "123" 1M) ]

        (s/save-liikennesuoritteet! id [liikennesuorite]) =>
          (throws "Liikennetyyppiä 123 ei ole olemassa.")))

    (fact "Liikennesuoritteen tallentaminen - kaksi samaa riviä"
      (let [id (hc/add-hakemus! hsl-ah0-hakemus)
            liikennesuorite (liikennesuorite id 1M) ]

        (s/save-liikennesuoritteet! id [liikennesuorite liikennesuorite]) =>
          (throws (str "Liikennesuorite PSA-1 on jo olemassa hakemuksella (id = " id ")."))))))




