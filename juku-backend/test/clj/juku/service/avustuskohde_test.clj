(ns juku.service.avustuskohde-test
  (:require [midje.sweet :refer :all]
            [clojure.string :as str]
            [common.collection :as c]
            [juku.service.hakemus :as h]
            [juku.service.avustuskohde :as ak]
            [juku.service.asiahallinta-mock :as asha]
            [juku.service.test :as test]
            [clj-http.fake :as fake]))

(defn find-by-id [id] (fn [m] (= (:id m) id)))

(def hakemuskausi (test/next-hakemuskausi!))
(def vuosi (:vuosi hakemuskausi))

(defn assoc-alv [ak] (assoc ak :alv 24))

(facts "-- Avustuskohteiden testit --"
(test/with-user "juku_hakija" ["juku_hakija"]
  (fact "Avustuskohteen lisääminen"
    (let [organisaatioid 1
          hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid organisaatioid}

          id (h/add-hakemus! hakemus)
          avustuskohde {:hakemusid id, :avustuskohdeluokkatunnus "PSA", :avustuskohdelajitunnus "1", :haettavaavustus 1M, :omarahoitus 1M}]

      (ak/add-avustuskohde! avustuskohde)

      (ak/find-avustuskohteet-by-hakemusid id) => [(assoc-alv avustuskohde)]))

  (fact "Avustuskohteen lisääminen - avustuskohde on jo olemassa"
    (let [organisaatioid 1
          hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid organisaatioid}

          id (h/add-hakemus! hakemus)
          avustuskohde {:hakemusid id, :avustuskohdeluokkatunnus "PSA", :avustuskohdelajitunnus "1", :haettavaavustus 1M, :omarahoitus 1M}]

      (ak/add-avustuskohde! avustuskohde)
      (ak/add-avustuskohde! avustuskohde) => (throws (str "Avustuskohde PSA-1 on jo olemassa hakemuksella (id = " id ")." ))))

  (fact "Avustuskohteen lisääminen - hakemusta ei löydy"
    (let [avustuskohde {:hakemusid 1324123434, :avustuskohdeluokkatunnus "PSA", :avustuskohdelajitunnus "1", :haettavaavustus 1M, :omarahoitus 1M}]

      (ak/add-avustuskohde! avustuskohde) => (throws "Avustuskohteen PSA-1 hakemusta (id = 1324123434) ei ole olemassa.")))

  (fact "Avustuskohteen tallentaminen ja hakeminen - uusi avustukohde"
    (let [organisaatioid 1
          hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid organisaatioid}

          id (h/add-hakemus! hakemus)
          avustuskohde {:hakemusid id, :avustuskohdeluokkatunnus "PSA", :avustuskohdelajitunnus "1", :haettavaavustus 1M, :omarahoitus 1M}]

        (ak/save-avustuskohteet![avustuskohde])
        (ak/find-avustuskohteet-by-hakemusid id) => [(assoc-alv avustuskohde)]))

  (fact "Avustuskohteiden päivittäminen"
     (let [organisaatioid 1
           hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid organisaatioid}

           id (h/add-hakemus! hakemus)
           ak1 {:hakemusid id, :avustuskohdeluokkatunnus "PSA", :avustuskohdelajitunnus "1", :haettavaavustus 1M, :omarahoitus 1M}
           ak2 (assoc ak1 :avustuskohdelajitunnus "2")]

       (:muokkaaja (h/get-hakemus+ id)) => nil
       (ak/save-avustuskohteet![ak1 ak2])

       (:muokkaaja (h/get-hakemus+ id)) => nil
       (ak/find-avustuskohteet-by-hakemusid id) => (map assoc-alv [ak1 ak2])
       (Thread/sleep 1000)
       (ak/save-avustuskohteet![ak1 (assoc ak2 :haettavaavustus 2M)])
       (:muokkaaja (h/get-hakemus+ id)) => "Harri Helsinki"

       (ak/find-avustuskohteet-by-hakemusid id) => (map assoc-alv [ak1 (assoc ak2 :haettavaavustus 2M)])))))

