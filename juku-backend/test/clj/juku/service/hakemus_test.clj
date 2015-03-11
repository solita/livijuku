(ns juku.service.hakemus_test
  (:require [midje.sweet :refer :all]
            [common.collection :as c]
            [juku.service.hakemus :as h]
            [juku.service.test :as test]))

(defn find-by-id [id] (fn [m] (= (:id m) id)))

(defn diaarinumero [id vuosi] (str "LIVI/" id "/07.00.01/" vuosi))

(let [hakemuskausi (test/next-hakemuskausi!)
      vuosi (:vuosi hakemuskausi)
      hakuaika (:hakuaika (test/hakemus-summary hakemuskausi "AH0"))
      assoc-hakemus-defaults (fn [hakemus id]
        (assoc hakemus :id id, :hakemustilatunnus "K", :diaarinumero (diaarinumero id vuosi), :hakuaika hakuaika))]

(facts "-- Hakemus testit --"

  (fact "Uuden hakemuksen luonti"
    (let [organisaatioid 1M
          hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid organisaatioid}
          id (h/add-hakemus! hakemus)]

      (dissoc (h/get-hakemus-by-id id) :muokkausaika :selite) => (assoc-hakemus-defaults hakemus id)))

  (fact "Uuden hakemuksen luonti - organisaatio ei ole olemassa"
    (let [organisaatioid 23453453453453
          hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0"
                   :organisaatioid organisaatioid}]

      (h/add-hakemus! hakemus) => (throws (str "Hakemuksen organisaatiota " organisaatioid " ei ole olemassa." ))))

  (fact "Hakemuksen selitteen päivittäminen"
        (let [organisaatioid 1M
              hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid organisaatioid}
              id (h/add-hakemus! hakemus)
              selite "selite"]

          (h/save-hakemus-selite! id selite)
          (dissoc (h/get-hakemus-by-id id) :muokkausaika) => (assoc (assoc-hakemus-defaults hakemus id) :selite selite)))

  (fact "Organisaation hakemusten haku"
      (let [organisaatioid 1M
            hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid organisaatioid}
            id (h/add-hakemus! hakemus)]

        (dissoc (c/find-first (find-by-id id) (h/find-organisaation-hakemukset organisaatioid)) :muokkausaika)
          => (assoc-hakemus-defaults hakemus id))))

(facts "-- Avustuskohteiden testit --"

  (fact "Avustuskohteen lisääminen"
    (let [organisaatioid 1
          hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid organisaatioid}

          id (h/add-hakemus! hakemus)
          avustuskohde {:hakemusid id, :avustuskohdelajitunnus "PSA-1", :haettavaavustus 1M, :omarahoitus 1M}]

      (h/add-avustuskohde! avustuskohde)
      (h/find-avustuskohteet-by-hakemusid id) => [avustuskohde]))

  (fact "Avustuskohteen lisääminen - avustuskohde on jo olemassa"
    (let [organisaatioid 1
          hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid organisaatioid}

          id (h/add-hakemus! hakemus)
          avustuskohde {:hakemusid id, :avustuskohdelajitunnus "PSA-1", :haettavaavustus 1M, :omarahoitus 1M}]

      (h/add-avustuskohde! avustuskohde)
      (h/add-avustuskohde! avustuskohde) => (throws (str "Avustuskohde PSA-1 on jo olemassa hakemuksella (id = " id ")." ))))

  (fact "Avustuskohteen lisääminen - hakemusta ei löydy"
    (let [avustuskohde {:hakemusid 1324123434, :avustuskohdelajitunnus "PSA-1", :haettavaavustus 1M, :omarahoitus 1M}]

      (h/add-avustuskohde! avustuskohde) => (throws "Avustuskohteen PSA-1 hakemusta (id = 1324123434) ei ole olemassa.")))

  (fact "Avustuskohteiden tallentaminen ja hakeminen"
    (let [organisaatioid 1
          hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid organisaatioid}

          id (h/add-hakemus! hakemus)
          avustuskohde {:hakemusid id, :avustuskohdelajitunnus "PSA-1", :haettavaavustus 1M, :omarahoitus 1M}]

        (h/save-avustuskohteet![avustuskohde])
        (h/find-avustuskohteet-by-hakemusid id) => [avustuskohde]))))

