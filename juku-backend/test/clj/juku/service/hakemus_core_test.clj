(ns juku.service.hakemus-core-test
  (:require [midje.sweet :refer :all]
            [clojure.string :as str]
            [common.collection :as coll]
            [clojure.tools.logging :as log]
            [common.map :as m]
            [clj-time.core :as time]
            [clj-time.format :as timef]
            [juku.db.coerce :as dbc]
            [juku.service.hakemus-core :as h]
            [juku.service.hakemuskausi :as hk]
            [juku.service.liitteet :as l]
            [juku.service.avustuskohde :as ak]
            [juku.service.asiahallinta-mock :as asha]
            [juku.service.test :as test]
            [juku.headers :as headers]
            [common.core :as c]))

(defn find-by-id [id] (fn [m] (= (:id m) id)))

(def hakemuskausi (test/next-hakemuskausi!))
(def vuosi (:vuosi hakemuskausi))
(def hakuaika (:hakuaika (test/hakemus-summary hakemuskausi "AH0")))
(hk/update-hakemuskausi-set-diaarinumero! {:vuosi vuosi :diaarinumero (str "dnro:" vuosi)})

(def hsl-hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid 1M})

(defn assoc-hakemus-defaults [hakemus id]
  (assoc hakemus :id id, :hakemustilatunnus "K", :diaarinumero nil, :hakuaika hakuaika))

(defn assoc-hakemus-defaults+kasittely [hakemus id]
  (assoc (assoc-hakemus-defaults hakemus id) :kasittelijanimi nil))

(defn assoc-hakemus-defaults+ [hakemus id selite]
  (assoc (assoc-hakemus-defaults+kasittely hakemus id) :contentvisible true
                                                       :luontitunnus "juku_kasittelija",
                                                       :kasittelija nil, :selite selite,
                                                       :tilinumero "asdf",
                                                       :muokkaaja nil, :lahettaja nil, :lahetysaika nil))

(defn expected-hakemussuunnitelma [id hakemus haettu-avustus myonnettava-avustus]
  (assoc (assoc-hakemus-defaults hakemus id) :haettu-avustus haettu-avustus :myonnettava-avustus myonnettava-avustus))

(defn dissoc-muokkausaika [obj] (dissoc obj :muokkausaika))

(log/info (str "user.timezone: " (get (System/getProperties) "user.timezone")))

;; ************ Hakemuksen käsittely ja haut ***********

(facts "Hakemuksen käsittely ja haut"

(test/with-user "juku_kasittelija" ["juku_kasittelija"]

  (fact "Uuden hakemuksen luonti"
    (let [organisaatioid 1M
          id (h/add-hakemus! hsl-hakemus)]

      (test/with-user "juku_hakija" ["juku_hakija"] (h/save-hakemus-tilinumero! id "asdf"))
      (dissoc (h/get-hakemus+ id) :muokkausaika :other-hakemukset) => (assoc-hakemus-defaults+ hsl-hakemus id nil)))

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
      (dissoc (h/get-hakemus+ id) :muokkausaika :other-hakemukset) => (assoc-hakemus-defaults+ hakemus id selite)))

  (fact "Hakemuksen selitteen päivittäminen - yli 32000 merkkiä ja sisältää ei ascii merkkejä.
         Oraclessa on tyypillisesti kaksi kynnysarvoa merkkijonojen käsittelyssä, jotka kannattaa testata 4K ja 32K."

    (let [hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid 1M}
          id (h/add-hakemus! hakemus)
          selite (str/join (take 4000 (repeat "selite-äöå-âãä")))]

      (h/save-hakemus-selite! id selite)
      (dissoc (h/get-hakemus+ id) :muokkausaika :other-hakemukset) => (assoc-hakemus-defaults+ hakemus id selite)))

  (fact "Organisaation hakemusten haku"
    (let [organisaatioid 1M
          hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid organisaatioid}
          id (h/add-hakemus! hakemus)
          organisaation-hakemukset  (h/find-organisaation-hakemukset organisaatioid)]

      ;; kaikki hakemukset ovat ko. organisaatiosta
      organisaation-hakemukset => (partial every? (coll/eq :organisaatioid organisaatioid))

      ;; hakemus (:id = id) löytyy hakutuloksista
      (dissoc-muokkausaika (coll/find-first (find-by-id id) organisaation-hakemukset))
        => (assoc-hakemus-defaults hakemus id)))

  (fact "Kaikkien hakemusten haku"
    (let [organisaatioid 1M
          id (h/add-hakemus! hsl-hakemus)
          hakemukset (h/find-all-hakemukset)]

      (count hakemukset) => (:count (first (test/select-count-hakemus)))

      ;; hakemus (:id = id) löytyy hakutuloksista
      (dissoc-muokkausaika (coll/find-first (find-by-id id) hakemukset))
        => (assoc-hakemus-defaults+kasittely hsl-hakemus id)))))


(test/with-user "juku_kasittelija" ["juku_kasittelija"]
  (fact "Hakemussuunnitelmien haku - ei avustuskohteita"
    (let [hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid 1M}
          id (h/add-hakemus! hakemus)
          hakemussuunnitelmat  (h/find-hakemussuunnitelmat vuosi, "AH0")]

      ;; kaikki hakemukset ovat samalta vuodelta
      hakemussuunnitelmat => (partial every? (coll/eq :vuosi vuosi))

      ;; kaikki hakemukset ovat samaa tyyppiä
      hakemussuunnitelmat => (partial every? (coll/eq :hakemustyyppitunnus "AH0"))

      ;; hakemus (:id = id) löytyy hakutuloksista
      (dissoc-muokkausaika (coll/find-first (find-by-id id) hakemussuunnitelmat))
        => (expected-hakemussuunnitelma id hakemus 0M 0M)))

  (fact "Hakemussuunnitelmien haku - lisätty avustuskohde ja myönnetty avustus"
    (let [hakemus (fn [organisaatioid] {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid organisaatioid})
          id1 (h/add-hakemus! (hakemus 1M))
          id2 (h/add-hakemus! (hakemus 2M))
          avustuskohde (fn [hakemusid] {:hakemusid hakemusid, :avustuskohdeluokkatunnus "PSA", :avustuskohdelajitunnus "1", :haettavaavustus 2M, :omarahoitus 2M})]

      ;; aseta haetut avustukset
      (ak/add-avustuskohde! (avustuskohde id1))
      (ak/add-avustuskohde! (avustuskohde id2))

      ;; aseta myonnettava avustus
      (h/save-hakemus-suunniteltuavustus! id1 1)
      (h/save-hakemus-suunniteltuavustus! id2 1)

      (let [hakemussuunnitelmat  (h/find-hakemussuunnitelmat vuosi, "AH0")]

          ;; kaikki hakemukset ovat samalta vuodelta
          hakemussuunnitelmat => (partial every? (coll/eq :vuosi vuosi))

          ;; kaikki hakemukset ovat samaa tyyppiä
          hakemussuunnitelmat => (partial every? (coll/eq :hakemustyyppitunnus "AH0"))

          ;; hakemus (:id = id) löytyy hakutuloksista
          (dissoc (coll/find-first (find-by-id id1) hakemussuunnitelmat) :muokkausaika)
            => (expected-hakemussuunnitelma id1 (hakemus 1M) 2M 1M)

          ;; hakemus (:id = id) löytyy hakutuloksista
          (dissoc-muokkausaika (coll/find-first (find-by-id id2) hakemussuunnitelmat))
            => (expected-hakemussuunnitelma id2 (hakemus 2M) 2M 1M))))

  (fact "Hakemussuunnitelmien haku - HK kohde"
    (let [hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid 1M}
          id (h/add-hakemus! hakemus)]

      (ak/add-avustuskohde! {:hakemusid id, :avustuskohdeluokkatunnus "HK", :avustuskohdelajitunnus "SL", :haettavaavustus 1M, :omarahoitus 1M})
      (ak/add-avustuskohde! {:hakemusid id, :avustuskohdeluokkatunnus "HK", :avustuskohdelajitunnus "KL", :haettavaavustus 1.95M, :omarahoitus 1M})

      (let [hakemussuunnitelmat  (h/find-hakemussuunnitelmat vuosi, "AH0")]
        ;; kaikki hakemukset ovat samalta vuodelta
        hakemussuunnitelmat => (partial every? (coll/eq :vuosi vuosi))

        ;; kaikki hakemukset ovat samaa tyyppiä
        hakemussuunnitelmat => (partial every? (coll/eq :hakemustyyppitunnus "AH0"))

        ;; hakemus (:id = id) löytyy hakutuloksista
        (dissoc-muokkausaika (coll/find-first (find-by-id id) hakemussuunnitelmat))
        => (expected-hakemussuunnitelma id hakemus 3.25M 0M)))))

(facts "Ei käynnissä tilan käsittely"

  (fact "Keskeneräisen hakemuksen tila ennen hakuajan alkua on 0 (ei käynnissä)"
    (let [vuosi (:vuosi (test/next-hakemuskausi!))
          hakuaika {:alkupvm (test/from-today 1)
                    :loppupvm (test/from-today 2)}
          hakuajat [(assoc hakuaika :hakemustyyppitunnus "AH0")]
          id (h/add-hakemus! {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid 1M})]

      (hk/save-hakemuskauden-hakuajat! vuosi hakuajat)
      (:hakemustilatunnus (h/get-hakemus+ id)) => "0"))

  (fact "Keskeneräisen hakemuksen tila hakuajan alkamisen jälkeen on K (keskeneräinen)"
        (let [vuosi (:vuosi (test/next-hakemuskausi!))
              hakuaika {:alkupvm (test/before-today 1)
                        :loppupvm (test/from-today 1)}
              hakuajat [(assoc hakuaika :hakemustyyppitunnus "AH0")]
              id (h/add-hakemus! {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid 1M})]

          (hk/save-hakemuskauden-hakuajat! vuosi hakuajat)
          (:hakemustilatunnus (h/get-hakemus+ id)) => "0")))

(defn avustuskohde-psa1 [hakemusid]
  {:hakemusid hakemusid, :avustuskohdeluokkatunnus "PSA", :avustuskohdelajitunnus "1", :haettavaavustus 1M, :omarahoitus 1M})

(fact "Kaikkien hakemusten haku - sisällön muokkaus muuttaa muokkausaikaa"
  (test/with-user "juku_hakija" ["juku_hakija"]
    (let [id (h/add-hakemus! hsl-hakemus)
          hakemukset (h/find-all-hakemukset)
          hakemus (coll/find-first (find-by-id id) hakemukset)
          original-muokkausaika (:muokkausaika hakemus)]

      (count hakemukset) => (:count (first (test/select-count-hakemus)))

      (dissoc-muokkausaika hakemus) => (assoc-hakemus-defaults+kasittely hsl-hakemus id)
      (Thread/sleep 1000)
      (ak/save-avustuskohteet! [(avustuskohde-psa1 id)])

      (coll/find-first (find-by-id id) (h/find-all-hakemukset)) =>
        (coll/predicate time/after? :muokkausaika original-muokkausaika))))

(fact
  "Hakemuksen tilinumeron päivittäminen"
  (test/with-user "juku_hakija" ["juku_hakija"]
    (let [hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid 1M}
          id (h/add-hakemus! hakemus)
          tilinumero "asdf"]

      (h/save-hakemus-tilinumero! id tilinumero)
      (dissoc (h/get-hakemus+ id) :muokkausaika :other-hakemukset) =>
        (assoc (assoc-hakemus-defaults+ hakemus id nil)
          :tilinumero tilinumero
          :luontitunnus "juku_hakija"))))

(fact
  "Hakemuksen oletustilinumero"
  (test/with-user "juku_hakija" ["juku_hakija"]
    (let [vuosi (:vuosi (test/next-hakemuskausi!))
          hakemus {:vuosi vuosi :hakemustyyppitunnus "MH1" :organisaatioid 1M}
          id1 (h/add-hakemus! {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid 1M})
          id2 (h/add-hakemus! hakemus)
          tilinumero "asdf1234"]

      (h/save-hakemus-tilinumero! id1 tilinumero)
      (:tilinumero (h/get-hakemus+ id2)) => tilinumero)))