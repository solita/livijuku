(ns juku.service.avustuskohde-test
  (:require [midje.sweet :refer :all]
            [clojure.string :as str]
            [common.collection :as c]
            [juku.service.hakemus-core :as h]
            [juku.service.avustuskohde :as ak]
            [juku.service.asiahallinta-mock :as asha]
            [juku.service.test :as test]
            [clj-http.fake :as fake]))

(defn find-by-id [id] (fn [m] (= (:id m) id)))

(def hakemuskausi (test/next-hakemuskausi!))
(def vuosi (:vuosi hakemuskausi))
(def hsl-ah0-hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid 1M})

(defn assoc-10alv [ak] (assoc ak :alv 10))
(defn assoc-24alv [ak] (assoc ak :alv 24))

; -- Avustuskohteiden testit --

(facts "Avustuskohteiden testit"
(test/with-user "juku_hakija" ["juku_hakija"]
  (fact "Avustuskohteen lisääminen - alv10"
    (let [id (h/add-hakemus! hsl-ah0-hakemus)
          avustuskohde {:hakemusid id, :avustuskohdeluokkatunnus "PSA", :avustuskohdelajitunnus "1", :haettavaavustus 1M, :omarahoitus 1M}]

      (ak/add-avustuskohde! avustuskohde)

      (ak/find-avustuskohteet-by-hakemusid id) => [(assoc-10alv avustuskohde)]))

  (fact "Avustuskohteen lisääminen - maksimirahamäärä"
    (let [id (h/add-hakemus! hsl-ah0-hakemus)
          avustuskohde {:hakemusid id, :avustuskohdeluokkatunnus "PSA", :avustuskohdelajitunnus "1", :haettavaavustus 999999999.99M, :omarahoitus 999999999.99M}]

      (ak/add-avustuskohde! avustuskohde)

      (ak/find-avustuskohteet-by-hakemusid id) => [(assoc-10alv avustuskohde)]))

  (fact "Avustuskohteen lisääminen - alv24"
    (let [id (h/add-hakemus! hsl-ah0-hakemus)
          avustuskohde {:hakemusid id, :avustuskohdeluokkatunnus "K", :avustuskohdelajitunnus "M", :haettavaavustus 1M, :omarahoitus 1M}]

      (ak/add-avustuskohde! avustuskohde)

      (ak/find-avustuskohteet-by-hakemusid id) => [(assoc-24alv avustuskohde)]))

  (fact "Avustuskohteen tallentaminen ja hakeminen - uusi avustukohde"
    (let [id (h/add-hakemus! hsl-ah0-hakemus)
          avustuskohde {:hakemusid id, :avustuskohdeluokkatunnus "PSA", :avustuskohdelajitunnus "1", :haettavaavustus 1M, :omarahoitus 1M}]

        (ak/save-avustuskohteet![avustuskohde])
        (ak/find-avustuskohteet-by-hakemusid id) => [(assoc-10alv avustuskohde)]))

  (fact "Avustuskohteiden päivittäminen"
     (let [id (h/add-hakemus! hsl-ah0-hakemus)
           ak1 {:hakemusid id, :avustuskohdeluokkatunnus "PSA", :avustuskohdelajitunnus "1", :haettavaavustus 1M, :omarahoitus 1M}
           ak2 (assoc ak1 :avustuskohdelajitunnus "2")]

       (:muokkaaja (h/get-hakemus+ id)) => nil
       (ak/save-avustuskohteet![ak1 ak2])

       (:muokkaaja (h/get-hakemus+ id)) => nil
       (ak/find-avustuskohteet-by-hakemusid id) => (map assoc-10alv [ak1 ak2])
       (Thread/sleep 1000)
       (ak/save-avustuskohteet![ak1 (assoc ak2 :haettavaavustus 2M)])
       (:muokkaaja (h/get-hakemus+ id)) => "Harri Helsinki"

       (ak/find-avustuskohteet-by-hakemusid id) => (map assoc-10alv [ak1 (assoc ak2 :haettavaavustus 2M)])))))

(facts "Avustuskohteiden testit - virhetilanteet"
  (test/with-user "juku_hakija" ["juku_hakija"]
    (fact "Avustuskohteen lisääminen - avustuskohde on jo olemassa"
      (let [id (h/add-hakemus! hsl-ah0-hakemus)
           avustuskohde {:hakemusid id, :avustuskohdeluokkatunnus "PSA", :avustuskohdelajitunnus "1", :haettavaavustus 1M, :omarahoitus 1M}]

       (ak/add-avustuskohde! avustuskohde)
       (ak/add-avustuskohde! avustuskohde) => (throws (str "Avustuskohde PSA-1 on jo olemassa hakemuksella (id = " id ")." ))))

    (fact "Avustuskohteen lisääminen - hakemusta ei löydy"
      (let [avustuskohde {:hakemusid 1324123434, :avustuskohdeluokkatunnus "PSA", :avustuskohdelajitunnus "1", :haettavaavustus 1M, :omarahoitus 1M}]

        (ak/add-avustuskohde! avustuskohde) => (throws "Avustuskohteen PSA-1 hakemusta (id = 1324123434) ei ole olemassa.")))

    (fact "Avustuskohteen lisääminen - avustuskohdelajia ei ole olemassa"
      (let [id (h/add-hakemus! hsl-ah0-hakemus)
            avustuskohde {:hakemusid id, :avustuskohdeluokkatunnus "asd", :avustuskohdelajitunnus "1", :haettavaavustus 1M, :omarahoitus 1M}]

        (ak/add-avustuskohde! avustuskohde) => (throws (str "Avustuskohdelajia asd-1 ei ole olemassa." ))))

    (fact "Avustuskohteen lisääminen - rahamäärä liian suuri"
      (let [id (h/add-hakemus! hsl-ah0-hakemus)
            avustuskohde {:hakemusid id, :avustuskohdeluokkatunnus "PSA", :avustuskohdelajitunnus "1", :haettavaavustus 1000000000M, :omarahoitus 1M}]

        (ak/add-avustuskohde! avustuskohde) => (throws #"Failed to execute: insert into avustuskohde.*" )))))


(fact "Avustuskohteen lisääminen - ei oma hakemus"
  (test/with-user "juku_kasittelija" ["juku_hakija"]
    (let [id (h/add-hakemus! hsl-ah0-hakemus)
          avustuskohde {:hakemusid id, :avustuskohdeluokkatunnus "PSA", :avustuskohdelajitunnus "1", :haettavaavustus 1M, :omarahoitus 1M}]

      (ak/save-avustuskohteet! [avustuskohde]) => (throws (str "Käyttäjä juku_kasittelija ei omista hakemuksia: " id)))))

(defn create-avustuskohde [hakemusid]
  {:hakemusid hakemusid, :avustuskohdeluokkatunnus "PSA", :avustuskohdelajitunnus "1", :haettavaavustus 1M, :omarahoitus 1M})

(facts "Avustuskohteiden haku ja käyttöoikeudet"
  (fact "Keskeneräinen oma hakemus"
    (test/with-user "juku_hakija" ["juku_hakija"]
      (let [id (h/add-hakemus! hsl-ah0-hakemus)
            avustuskohde (create-avustuskohde id)]

        (ak/add-avustuskohde! avustuskohde)
        (ak/find-avustuskohteet id) => [(assoc-10alv avustuskohde)])))

  (fact "Keskeneräinen hakemus - käyttäjä ei omista hakemusta eikä ole käsittelijä"
    (test/with-user "juku_hakija" ["juku_hakija"]
      (let [id (h/add-hakemus! {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid 2M})
            avustuskohde (create-avustuskohde id)]

       (ak/add-avustuskohde! avustuskohde)
       (ak/find-avustuskohteet id) =>
        (throws (str "Käyttäjällä juku_hakija ei ole oikeutta nähdä hakemuksen: " id " sisältöä. "
                     "Käyttäjä ei ole hakemuksen omistaja ja käyttäjällä ei ole oikeutta nähdä keskeneräisiä hakemuksia.")))))

  (fact "Keskeneräinen hakemus - käyttäjä on käsittelijä"
    (test/with-user "juku_kasittelija" ["juku_kasittelija"]
      (let [id (h/add-hakemus! hsl-ah0-hakemus)
           avustuskohde (create-avustuskohde id)]

       (ak/add-avustuskohde! avustuskohde)
       (ak/find-avustuskohteet id) => [(assoc-10alv avustuskohde)]))))
