(ns juku.service.hakemus-test
  (:require [midje.sweet :refer :all]
            [clojure.string :as str]
            [common.collection :as coll]
            [common.map :as m]
            [clj-time.core :as time]
            [clj-time.format :as timef]
            [juku.db.coerce :as dbc]
            [juku.service.hakemus :as h]
            [juku.service.hakemuskausi :as hk]
            [juku.service.liitteet :as l]
            [juku.service.avustuskohde :as ak]
            [juku.service.asiahallinta-mock :as asha]
            [juku.service.test :as test]
            [juku.headers :as headers]
            [clj-http.fake :as fake]))

(defn find-by-id [id] (fn [m] (= (:id m) id)))

(def hakemuskausi (test/next-hakemuskausi!))
(def vuosi (:vuosi hakemuskausi))
(def hakuaika (:hakuaika (test/hakemus-summary hakemuskausi "AH0")))
(hk/update-hakemuskausi-set-diaarinumero! {:vuosi vuosi :diaarinumero (str "dnro:" vuosi)})

(defn assoc-hakemus-defaults [hakemus id]
  (assoc hakemus :id id, :hakemustilatunnus "K", :diaarinumero nil, :hakuaika hakuaika))

(defn assoc-hakemus-defaults+ [hakemus id selite]
  (assoc (assoc-hakemus-defaults hakemus id) :luontitunnus "juku_kasittelija", :kasittelija nil :selite selite, :hakija nil))

(defn expected-hakemussuunnitelma [id hakemus haettu-avustus myonnettava-avustus]
  (assoc (assoc-hakemus-defaults hakemus id) :haettu-avustus haettu-avustus :myonnettava-avustus myonnettava-avustus))

;; ************ Hakemuksen käsittely ja haut ***********

(facts "Hakemuksen käsittely ja haut"

(test/with-user "juku_kasittelija" ["juku_kasittelija"]

  (fact "Uuden hakemuksen luonti"
    (let [organisaatioid 1M
          hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid organisaatioid}
          id (h/add-hakemus! hakemus)]

      (dissoc (h/get-hakemus-by-id id) :muokkausaika) => (assoc-hakemus-defaults+ hakemus id nil)))

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
      (dissoc (h/get-hakemus-by-id id) :muokkausaika) => (assoc-hakemus-defaults+ hakemus id selite)))

  (fact "Hakemuksen selitteen päivittäminen - yli 4000 merkkiä ja sisältää ei ascii merkkejä"
    (let [hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid 1M}
          id (h/add-hakemus! hakemus)
          selite (str/join (take 4000 (repeat "selite-äöå-âãä")))]

      (h/save-hakemus-selite! id selite)
      (dissoc (h/get-hakemus-by-id id) :muokkausaika) => (assoc-hakemus-defaults+ hakemus id selite)))

  (fact "Organisaation hakemusten haku"
    (let [organisaatioid 1M
          hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid organisaatioid}
          id (h/add-hakemus! hakemus)
          organisaation-hakemukset  (h/find-organisaation-hakemukset organisaatioid)]

      ;; kaikki hakemukset ovat ko. organisaatiosta
      organisaation-hakemukset => (partial every? (coll/eq :organisaatioid organisaatioid))

      ;; hakemus (:id = id) löytyy hakutuloksista
      (dissoc (coll/find-first (find-by-id id) organisaation-hakemukset) :muokkausaika)
        => (assoc-hakemus-defaults hakemus id)))

  (fact "Hakemussuunnitelmien haku"
    (let [hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid 1M}
          id (h/add-hakemus! hakemus)
          hakemussuunnitelmat  (h/find-hakemussuunnitelmat vuosi, "AH0")]

      ;; kaikki hakemukset ovat samalta vuodelta
      hakemussuunnitelmat => (partial every? (coll/eq :vuosi vuosi))

      ;; kaikki hakemukset ovat samaa tyyppiä
      hakemussuunnitelmat => (partial every? (coll/eq :hakemustyyppitunnus "AH0"))

      ;; hakemus (:id = id) löytyy hakutuloksista
      (dissoc (coll/find-first (find-by-id id) hakemussuunnitelmat) :muokkausaika)
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
          (dissoc (coll/find-first (find-by-id id2) hakemussuunnitelmat) :muokkausaika)
            => (expected-hakemussuunnitelma id2 (hakemus 2M) 2M 1M))))))

(facts "Ei käynnissä tilan käsittely"

  (fact "Keskeneräisen hakemuksen tila ennen hakuajan alkua on 0 (ei käynnissä)"
    (let [vuosi (:vuosi (test/next-hakemuskausi!))
          hakuaika {:alkupvm (test/from-today 1)
                    :loppupvm (test/from-today 2)}
          hakuajat [(assoc hakuaika :hakemustyyppitunnus "AH0")]
          id (h/add-hakemus! {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid 1M})]

      (hk/save-hakemuskauden-hakuajat! vuosi hakuajat)
      (:hakemustilatunnus (h/get-hakemus-by-id id)) => "0"))

  (fact "Keskeneräisen hakemuksen tila hakuajan alkamisen jälkeen on K (keskeneräinen)"
        (let [vuosi (:vuosi (test/next-hakemuskausi!))
              hakuaika {:alkupvm (test/before-today 1)
                        :loppupvm (test/from-today 1)}
              hakuajat [(assoc hakuaika :hakemustyyppitunnus "AH0")]
              id (h/add-hakemus! {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid 1M})]

          (hk/save-hakemuskauden-hakuajat! vuosi hakuajat)
          (:hakemustilatunnus (h/get-hakemus-by-id id)) => "0")))

;; ************ Hakemuksen tilan hallinta ***********

(def hsl-hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid 1M})

(facts "Hakemuksen tilan hallinta - asiahallinta testit"

  (test/with-user "juku_hakija" ["juku_hakija"]
    (fact "Hakemuksen lähettäminen"
      (asha/with-asha
        (let [id (h/add-hakemus! hsl-hakemus)]

          (h/laheta-hakemus! id)

          (asha/headers :vireille) => asha/valid-headers?
          (:diaarinumero (h/get-hakemus-by-id id)) => "testing")))

    (fact "Hakemuksen lähettäminen - liitteet"
      (asha/with-asha
        (let [id (h/add-hakemus! hsl-hakemus)
             liite1 {:hakemusid id :nimi "t-1" :contenttype "text/plain"}
             liite2 {:hakemusid id :nimi "t-åäö" :contenttype "text/plain"}]

           (l/add-liite! liite1 (test/inputstream-from "test-1"))
           (l/add-liite! liite2 (test/inputstream-from "test-2"))

           (h/laheta-hakemus! id)
           (asha/headers :vireille) => asha/valid-headers?

           (let [request (asha/request :vireille)
                 multipart (m/map-values first (group-by (coll/or* :part-name :name) (:multipart request)))
                 hakemus-asiakirja (get multipart "hakemus-asiakirja")]

             (get-in multipart ["hakemus" :content]) =>
                (str "{\"omistavaHenkilo\":\"test\","
                      "\"omistavaOrganisaatio\":\"Liikennevirasto\","
                      "\"kausi\":\"dnro:" vuosi "\","
                      "\"hakija\":\"Helsingin seudun liikenne\"}")

             (:mime-type hakemus-asiakirja) => "application/pdf"
             (:part-name hakemus-asiakirja) => "hakemus-asiakirja"
             (:name hakemus-asiakirja) => "hakemus.pdf"

             (slurp (get-in multipart ["t-1" :content])) => "test-1"
             (slurp (get-in multipart [(headers/encode-value "t-åäö") :content])) => "test-2")

           (:diaarinumero (h/get-hakemus-by-id id)) => "testing")))

    (fact "Täydennyspyynnön lähettäminen"
      (asha/with-asha
        (let [id (h/add-hakemus! hsl-hakemus)]

          (h/laheta-hakemus! id)
          (h/taydennyspyynto! id)

          (:hakemustilatunnus (h/get-hakemus-by-id id)) => "T0"

          (asha/headers :taydennyspyynto) => asha/valid-headers?
          (:uri (asha/request :taydennyspyynto)) => "/api/hakemus/testing/taydennyspyynto"

          (let [maarapvm (dbc/date->datetime (:maarapvm (first (test/select-maarapvm {:hakemusid id :nro 1}))))
                maarapvm-json (timef/unparse (:date-time timef/formatters) maarapvm)]

            (slurp (:body (asha/request :taydennyspyynto))) =>
              (str "{\"maaraaika\":\"" maarapvm-json "\","
                    "\"kasittelija\":\"Harri Helsinki\",\"hakija\":\"Helsingin seudun liikenne\"}")))))

    (fact "Täydennyksen lähettäminen"
      (asha/with-asha
       (let [id (h/add-hakemus! hsl-hakemus)]

         (h/laheta-hakemus! id)
         (h/taydennyspyynto! id)
         (h/laheta-taydennys! id)

         (:hakemustilatunnus (h/get-hakemus-by-id id)) => "TV"

         (asha/headers :taydennys) => asha/valid-headers?

         (let [request (asha/request :taydennys)
               multipart (m/map-values first (group-by (coll/or* :part-name :name) (:multipart request)))
               hakemus-asiakirja (get multipart "hakemus-asiakirja")]

           (:uri request) => "/api/hakemus/testing/taydennys"

           (get-in multipart ["taydennys" :content]) =>
             (str "{\"kasittelija\":\"Harri Helsinki\",\"lahettaja\":\"Helsingin seudun liikenne\"}")

           (:mime-type hakemus-asiakirja) => "application/pdf"
           (:part-name hakemus-asiakirja) => "hakemus-asiakirja"
           (:name hakemus-asiakirja) => "hakemus.pdf"))))

    (fact "Tarkastaminen"
      (asha/with-asha
       (let [id (h/add-hakemus! hsl-hakemus)]

         (h/laheta-hakemus! id)
         (h/tarkasta-hakemus! id)

         (:hakemustilatunnus (h/get-hakemus-by-id id)) => "T"

         (asha/headers :tarkastettu) => asha/valid-headers?
         (:uri (asha/request :tarkastettu))) => "/api/hakemus/testing/tarkastettu"))

  (fact "Maksatushakemuksen lähettäminen"
    (asha/with-asha
      (let [id1 (h/add-hakemus! hsl-hakemus)
            id2 (h/add-hakemus! (assoc hsl-hakemus :hakemustyyppitunnus "MH1"))]

        (h/laheta-hakemus! id1)
        (test/with-user "juku_kasittelija" ["juku_kasittelija"] (do (h/get-hakemus-by-id! id1)))
        (h/laheta-hakemus! id2)

        (:hakemustilatunnus (h/get-hakemus-by-id id1)) => "V"
        (:hakemustilatunnus (h/get-hakemus-by-id id2)) => "V"

        (let [request (asha/request :maksatushakemus)
              multipart (m/map-values first (group-by (coll/or* :part-name :name) (:multipart request)))
              hakemus-asiakirja (get multipart "hakemus-asiakirja")]

          (:headers request) => asha/valid-headers?
          (:uri request) => "/api/hakemus/testing/maksatushakemus"

          (get-in multipart ["maksatushakemus" :content]) =>
            (str "{\"kasittelija\":\"Katri Käsittelijä\",\"lahettaja\":\"Helsingin seudun liikenne\"}")

          (:mime-type hakemus-asiakirja) => "application/pdf"
          (:part-name hakemus-asiakirja) => "hakemus-asiakirja"
          (:name hakemus-asiakirja) => "hakemus.pdf"))))))

(defn assert-state-transition [expected-hakemustilatunnus operation operationname]
  (fact (str "Hakemuksen " operationname " tehdään väärässä tilassa K")
      (test/with-user "juku_kasittelija" ["juku_kasittelija"]
          (asha/with-asha-off
            (let [id (h/add-hakemus! hsl-hakemus)]

              (operation id) =>
              (throws (str "Hakemuksen (" id ") "
                           operationname " ei ole sallittu tilassa: K. Hakemuksen " operationname
                           " on sallittu vain tilassa: "
                           expected-hakemustilatunnus)))))))

(facts "Virheelliset tilasiirtymät"
       (assert-state-transition "V" h/tarkasta-hakemus! "tarkastaminen")
       (assert-state-transition "V" h/taydennyspyynto! "täydennyspyyntö")
       (assert-state-transition "T0" h/laheta-taydennys! "täydentäminen"))

(facts "Hakemuksen tilan hallinta - asiahallinta pois päältä"
  (fact "Hakemuksen lähettäminen - asiahallinta on pois päältä"
     (asha/with-asha-off
       (let [id (h/add-hakemus! hsl-hakemus)]

         (h/laheta-hakemus! id)
         (:diaarinumero (h/get-hakemus-by-id id)) => nil))))

(fact "Hakemuksen käsittelijän automaattinen asettaminen"
  (test/with-user "juku_kasittelija" ["juku_kasittelija"]
    (asha/with-asha
      (let [id (h/add-hakemus! hsl-hakemus)]

        (:kasittelija (h/get-hakemus-by-id! id)) => nil
        (h/laheta-hakemus! id)

        (:kasittelija (h/get-hakemus-by-id! id)) => nil
        (:kasittelija (h/get-hakemus-by-id! id)) => "juku_kasittelija"

        (:hakemustilatunnus (h/get-hakemus-by-id id)) => "V"

        (asha/headers :kasittely) => asha/valid-headers?
        (:uri (asha/request :kasittely)) => "/api/hakemus/testing/kasittely"))))

;; Määräpäivän laskennan testit

(facts "Määräpäivän laskenta"
       (fact (h/maarapvm (time/today)) => (test/from-today 14))
       (fact (h/maarapvm (test/before-today 1)) => (test/from-today 14))
       (fact (h/maarapvm (test/from-today 1)) => (test/from-today 14))
       (fact (h/maarapvm (test/from-today 13)) => (test/from-today 14))
       (fact (h/maarapvm (test/from-today 14)) => (test/from-today 14))
       (fact (h/maarapvm (test/from-today 15)) => (test/from-today 15))
       (fact (h/maarapvm (test/from-today 16)) => (test/from-today 16)))