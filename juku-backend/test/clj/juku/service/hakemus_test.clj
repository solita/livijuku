(ns juku.service.hakemus_test
  (:require [midje.sweet :refer :all]
            [clojure.string :as str]
            [common.collection :as c]
            [juku.service.hakemus :as h]
            [juku.service.asiahallinta-mock :as asha]
            [juku.service.test :as test]
            [clj-http.fake :as fake]))

(defn find-by-id [id] (fn [m] (= (:id m) id)))

(defn diaarinumero [id vuosi] (str "LIVI/" id "/07.00.01/" vuosi))

(def hakemuskausi (test/next-hakemuskausi!))
(def vuosi (:vuosi hakemuskausi))
(def hakuaika (:hakuaika (test/hakemus-summary hakemuskausi "AH0")))

(defn assoc-hakemus-defaults [hakemus id]
  (assoc hakemus :id id, :hakemustilatunnus "K", :diaarinumero nil, :hakuaika hakuaika))

(defn expected-hakemussuunnitelma [id hakemus haettu-avustus myonnettava-avustus]
  (assoc (assoc-hakemus-defaults hakemus id) :haettu-avustus haettu-avustus :myonnettava-avustus myonnettava-avustus))

;; ************ Hakemuksen käsittely ja haut ***********

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

  (fact "Uuden hakemuksen luonti - hakemuskausi ei ole olemassa"
    (let [organisaatioid 1M
          none-vuosi 9999
          hakemus {:vuosi none-vuosi :hakemustyyppitunnus "AH0"
                   :organisaatioid organisaatioid}]

      (h/add-hakemus! hakemus) => (throws (str "Hakemuskautta " none-vuosi " ei ole olemassa." ))))

  (fact "Hakemuksen selitteen päivittäminen"
    (let [hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid 1M}
          id (h/add-hakemus! hakemus)
          selite "selite"]

      (h/save-hakemus-selite! id selite)
      (dissoc (h/get-hakemus-by-id id) :muokkausaika) => (assoc (assoc-hakemus-defaults hakemus id) :selite selite)))

  (fact "Hakemuksen selitteen päivittäminen - yli 4000 merkkiä ja sisältää ei ascii merkkejä"
    (let [hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid 1M}
          id (h/add-hakemus! hakemus)
          selite (str/join (take 4000 (repeat "selite-äöå-âãä")))]

      (h/save-hakemus-selite! id selite)
      (dissoc (h/get-hakemus-by-id id) :muokkausaika) => (assoc (assoc-hakemus-defaults hakemus id) :selite selite)))

  (fact "Organisaation hakemusten haku"
    (let [organisaatioid 1M
          hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid organisaatioid}
          id (h/add-hakemus! hakemus)
          organisaation-hakemukset  (h/find-organisaation-hakemukset organisaatioid)]

      ;; kaikki hakemukset ovat ko. organisaatiosta
      organisaation-hakemukset => (partial every? (c/eq :organisaatioid organisaatioid))

      ;; hakemus (:id = id) löytyy hakutuloksista
      (dissoc (c/find-first (find-by-id id) organisaation-hakemukset) :muokkausaika)
        => (assoc-hakemus-defaults hakemus id)))

  (fact "Hakemussuunnitelmien haku"
    (let [hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid 1M}
          id (h/add-hakemus! hakemus)
          hakemussuunnitelmat  (h/find-hakemussuunnitelmat vuosi, "AH0")]

      ;; kaikki hakemukset ovat samalta vuodelta
      hakemussuunnitelmat => (partial every? (c/eq :vuosi vuosi))

      ;; kaikki hakemukset ovat samaa tyyppiä
      hakemussuunnitelmat => (partial every? (c/eq :hakemustyyppitunnus "AH0"))

      ;; hakemus (:id = id) löytyy hakutuloksista
      (dissoc (c/find-first (find-by-id id) hakemussuunnitelmat) :muokkausaika)
        => (expected-hakemussuunnitelma id hakemus 0M 0M)))

  (fact "Hakemussuunnitelmien haku - lisätty avustuskohde ja myönnetty avustus"
    (let [hakemus (fn [organisaatioid] {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid organisaatioid})
          id1 (h/add-hakemus! (hakemus 1M))
          id2 (h/add-hakemus! (hakemus 2M))
          avustuskohde (fn [hakemusid] {:hakemusid hakemusid, :avustuskohdeluokkatunnus "PSA", :avustuskohdelajitunnus "1", :haettavaavustus 2M, :omarahoitus 2M})]

      ;; aseta haetut avustukset
      (h/add-avustuskohde! (avustuskohde id1))
      (h/add-avustuskohde! (avustuskohde id2))

      ;; aseta myonnettava avustus
      (h/save-hakemus-suunniteltuavustus! id1 1)
      (h/save-hakemus-suunniteltuavustus! id2 1)

      (let [hakemussuunnitelmat  (h/find-hakemussuunnitelmat vuosi, "AH0")]

          ;; kaikki hakemukset ovat samalta vuodelta
          hakemussuunnitelmat => (partial every? (c/eq :vuosi vuosi))

          ;; kaikki hakemukset ovat samaa tyyppiä
          hakemussuunnitelmat => (partial every? (c/eq :hakemustyyppitunnus "AH0"))

          ;; hakemus (:id = id) löytyy hakutuloksista
          (dissoc (c/find-first (find-by-id id1) hakemussuunnitelmat) :muokkausaika)
            => (expected-hakemussuunnitelma id1 (hakemus 1M) 2M 1M)

          ;; hakemus (:id = id) löytyy hakutuloksista
          (dissoc (c/find-first (find-by-id id2) hakemussuunnitelmat) :muokkausaika)
            => (expected-hakemussuunnitelma id2 (hakemus 2M) 2M 1M)))))


(facts "Hakemuksen tilan hallinta - asiahallinta testit"
  (test/with-user "juku_kasittelija" ["juku_hakija"]
    (fake/with-fake-routes {#"http://(.+)/hakemus" (asha/asha-handler :vireille "testing\n")}
      (fact "Hakemuksen lähettäminen"
        (asha/with-asha
          (let [organisaatioid 1M
                hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid organisaatioid}
                id (h/add-hakemus! hakemus)]

            (h/laheta-hakemus! id)

            (asha/headers :vireille) => asha/valid-headers?
            (:diaarinumero (h/get-hakemus-by-id id)) => "testing"))))))


;; ************ Avustuskohteet ***********

(facts "-- Avustuskohteiden testit --"

  (fact "Avustuskohteen lisääminen"
    (let [organisaatioid 1
          hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid organisaatioid}

          id (h/add-hakemus! hakemus)
          avustuskohde {:hakemusid id, :avustuskohdeluokkatunnus "PSA", :avustuskohdelajitunnus "1", :haettavaavustus 1M, :omarahoitus 1M}]

      (h/add-avustuskohde! avustuskohde)
      (h/find-avustuskohteet-by-hakemusid id) => [avustuskohde]))

  (fact "Avustuskohteen lisääminen - avustuskohde on jo olemassa"
    (let [organisaatioid 1
          hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid organisaatioid}

          id (h/add-hakemus! hakemus)
          avustuskohde {:hakemusid id, :avustuskohdeluokkatunnus "PSA", :avustuskohdelajitunnus "1", :haettavaavustus 1M, :omarahoitus 1M}]

      (h/add-avustuskohde! avustuskohde)
      (h/add-avustuskohde! avustuskohde) => (throws (str "Avustuskohde PSA-1 on jo olemassa hakemuksella (id = " id ")." ))))

  (fact "Avustuskohteen lisääminen - hakemusta ei löydy"
    (let [avustuskohde {:hakemusid 1324123434, :avustuskohdeluokkatunnus "PSA", :avustuskohdelajitunnus "1", :haettavaavustus 1M, :omarahoitus 1M}]

      (h/add-avustuskohde! avustuskohde) => (throws "Avustuskohteen PSA-1 hakemusta (id = 1324123434) ei ole olemassa.")))

  (fact "Avustuskohteen tallentaminen ja hakeminen - uusi avustukohde"
    (let [organisaatioid 1
          hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid organisaatioid}

          id (h/add-hakemus! hakemus)
          avustuskohde {:hakemusid id, :avustuskohdeluokkatunnus "PSA", :avustuskohdelajitunnus "1", :haettavaavustus 1M, :omarahoitus 1M}]

        (h/save-avustuskohteet![avustuskohde])
        (h/find-avustuskohteet-by-hakemusid id) => [avustuskohde]))

  (fact "Avustuskohteiden päivittäminen"
     (let [organisaatioid 1
           hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid organisaatioid}

           id (h/add-hakemus! hakemus)
           ak1 {:hakemusid id, :avustuskohdeluokkatunnus "PSA", :avustuskohdelajitunnus "1", :haettavaavustus 1M, :omarahoitus 1M}
           ak2 (assoc ak1 :avustuskohdelajitunnus "2")]

       (h/save-avustuskohteet![ak1 ak2])
       (h/find-avustuskohteet-by-hakemusid id) => [ak1 ak2]

       (h/save-avustuskohteet![ak1 (assoc ak2 :haettavaavustus 2M)])
       (h/find-avustuskohteet-by-hakemusid id) => [ak1 (assoc ak2 :haettavaavustus 2M)])))

