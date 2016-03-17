(ns juku.service.seuranta-test
  (:require [midje.sweet :refer :all]
            [clojure.string :as str]
            [common.collection :as c]
            [juku.service.hakemus-core :as hc]
            [juku.service.hakemus :as h]
            [juku.service.seuranta :as s]
            [juku.service.avustuskohde :as ak]
            [juku.service.test :as test]
            [juku.service.asiahallinta-mock :as asha]
            [juku.service.pdf-mock :as pdf]
            [juku.service.hakemus-pdf-test :as hakemus-pdf]
            [juku.service.liitteet :as l]
            [common.string :as strx]))

(defn find-by-id [id] (fn [m] (= (:id m) id)))

(def hakemuskausi (test/next-avattu-empty-hakemuskausi!))
(def vuosi (:vuosi hakemuskausi))
(def hsl-ah0-hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid 1M})

(defn liikennesuorite
  ([numero] (liikennesuorite "PSA" numero))
  ([liikennetyyppitunnus numero] {
   :liikennetyyppitunnus liikennetyyppitunnus,
   :numero               numero,

   :suoritetyyppitunnus  "LS",
   :nimi                 "testilinja",
   :linjaautot           1M,
   :taksit               1M,
   :ajokilometrit        1M,
   :matkustajamaara      1M,
   :lipputulo            1M,
   :nettohinta           1M
}))

(defn lippusuorite
  ([numero] (lippusuorite "VL" numero nil))
  ([lipputyyppitunnus numero seutulippualue] {
  :lipputyyppitunnus lipputyyppitunnus,
  :numero               numero,

  :myynti 1M,
  :matkat 1M,
  :asiakashinta 1M,
  :keskipituus 1M,
  :lipputulo 1M,
  :julkinenrahoitus 1M,
  :seutulippualue seutulippualue
}))

; -- Liikennesuoritteiden testit --

(facts
  "Liikennesuoritteiden testit"
  (test/with-user
    "juku_hakija" ["juku_hakija"]
    (fact "Liikennesuoritteen tallentaminen ja haku"
      (let [id (hc/add-hakemus! hsl-ah0-hakemus)
            liikennesuorite (liikennesuorite 1M) ]

        (s/save-liikennesuoritteet! id [liikennesuorite])

        (s/find-hakemus-liikennesuoritteet id) => [liikennesuorite]))

    (fact "Usean liikennesuoritteen tallentaminen ja haku"
      (let [id (hc/add-hakemus! hsl-ah0-hakemus)
            l1 (liikennesuorite 1M)
            l2 (liikennesuorite 2M)]

        (s/save-liikennesuoritteet! id [l1 l2])

        (s/find-hakemus-liikennesuoritteet id) => [l1 l2]))

    (fact "Liikennesuoritteen päivittäminen ja haku"
      (let [id (hc/add-hakemus! hsl-ah0-hakemus)
            l1 (liikennesuorite 1M)
            l2 (liikennesuorite 2M)]

        (s/save-liikennesuoritteet! id [l1 l2])
        (s/save-liikennesuoritteet! id [l1])

        (s/find-hakemus-liikennesuoritteet id) => [l1]))))

(facts
  "Liikennesuoritteiden testit - virhetilanteet"
  (test/with-user
    "juku_hakija" ["juku_hakija"]
    (fact "Liikennesuoritteen tallentaminen - liikennetyyppiä ei ole olemassa"
      (let [id (hc/add-hakemus! hsl-ah0-hakemus)
            liikennesuorite (liikennesuorite "123" 1M) ]

        (s/save-liikennesuoritteet! id [liikennesuorite]) =>
          (throws "Liikennetyyppiä 123 ei ole olemassa.")))

    (fact "Liikennesuoritteen tallentaminen - kaksi samaa riviä"
      (let [id (hc/add-hakemus! hsl-ah0-hakemus)
            liikennesuorite (liikennesuorite 1M) ]

        (s/save-liikennesuoritteet! id [liikennesuorite liikennesuorite]) =>
          (throws (str "Liikennesuorite PSA-1 on jo olemassa hakemuksella (id = " id ")."))))))

; -- Lippusuoritteiden testit --

(facts
  "Lippusuoritteiden testit"
  (test/with-user
    "juku_hakija" ["juku_hakija"]
    (fact "Liikennesuoritteen tallentaminen ja haku"
          (let [id (hc/add-hakemus! hsl-ah0-hakemus)
                lippusuorite (lippusuorite 1M) ]

            (s/save-lippusuoritteet! id [lippusuorite])

            (s/find-hakemus-lippusuoritteet id) => [lippusuorite]))

    (fact "Usean lippusuoritteen tallentaminen ja haku"
          (let [id (hc/add-hakemus! hsl-ah0-hakemus)
                l1 (lippusuorite 1M)
                l2 (lippusuorite 2M)]

            (s/save-lippusuoritteet! id [l1 l2])

            (s/find-hakemus-lippusuoritteet id) => [l1 l2]))

    (fact "Liikennesuoritteen päivittäminen ja haku"
          (let [id (hc/add-hakemus! hsl-ah0-hakemus)
                l1 (lippusuorite 1M)
                l2 (lippusuorite 2M)]

            (s/save-lippusuoritteet! id [l1 l2])
            (s/save-lippusuoritteet! id [l1])

            (s/find-hakemus-lippusuoritteet id) => [l1]))))

(fact
  "Hakemuksen lähettäminen - seurantatietoliite"
  (test/with-user "juku_hakija" ["juku_hakija"]
    (asha/with-asha
      (let [ah0 (hc/add-hakemus! hsl-ah0-hakemus)
            id (hc/add-hakemus! {:vuosi vuosi :hakemustyyppitunnus "MH1" :organisaatioid 1M})
            liite1 {:hakemusid id :nimi "test" :contenttype "text/plain"}]

        (h/laheta-hakemus! ah0)
        (test/with-user "juku_kasittelija" ["juku_kasittelija"] (do (h/get-hakemus-by-id! ah0)))

        (:muokkaaja (hc/get-hakemus+ id)) => nil
        (l/add-liite! liite1 (test/inputstream-from "test"))
        (s/save-liikennesuoritteet! id [(liikennesuorite 1M) (liikennesuorite 2M)])
        (s/save-lippusuoritteet! id [(lippusuorite 1M) (lippusuorite 2M)])
        (:muokkaaja (hc/get-hakemus+ id)) => "Harri Helsinki"

        (h/laheta-hakemus! id)
        (asha/headers :vireille) => asha/valid-headers?
        (asha/headers :maksatushakemus) => asha/valid-headers?

        (let [request (asha/request :maksatushakemus)
              multipart (asha/group-by-multiparts request)
              hakemus-asiakirja (get multipart "hakemus-asiakirja")]

          (get-in multipart ["maksatushakemus" :content]) =>
          (str "{\"kasittelija\":\"Katri Käsittelijä\",\"lahettaja\":\"Helsingin seudun liikenne\"}")

          (:mime-type hakemus-asiakirja) => "application/pdf"
          (:part-name hakemus-asiakirja) => "hakemus-asiakirja"
          (:name hakemus-asiakirja) => "hakemus.pdf"

          (slurp (get-in multipart ["test" :content])) => "test"
          (let [seuranta-pdf (pdf/pdf->text (get-in multipart ["seurantatieto.pdf" :content]))]
            seuranta-pdf => (partial strx/substring? "Linjasuorite testilinja 1 1 1 1 1 1 2")
            seuranta-pdf => (partial strx/substring? "Vuosilippu 1 1 1 1 1 1")))

        (:diaarinumero (hc/get-hakemus+ id)) => "testing"))))



