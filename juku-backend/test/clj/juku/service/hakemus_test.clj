(ns juku.service.hakemus-test
  (:require [midje.sweet :refer :all]
            [clojure.string :as str]
            [common.collection :as coll]
            [clojure.tools.logging :as log]
            [common.map :as m]
            [clj-time.core :as time]
            [clj-time.format :as timef]
            [juku.db.coerce :as dbc]
            [juku.service.hakemus-core :as hc]
            [juku.service.hakemus :as h]
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

(log/info (str "user.timezone: " (get (System/getProperties) "user.timezone")))


;; ************ Hakemuksen tilan hallinta ***********

(def hsl-hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid 1M})

(facts "Hakemuksen tilan hallinta - asiahallinta testit"

  (test/with-user "juku_hakija" ["juku_hakija"]
    (fact "Hakemuksen lähettäminen"
      (asha/with-asha
        (let [id (hc/add-hakemus! hsl-hakemus)]

          (h/laheta-hakemus! id)

          (asha/headers :vireille) => asha/valid-headers?

          (let [hakemus (hc/get-hakemus+ id)]
            (:diaarinumero hakemus) => "testing"
            (:lahettaja hakemus) => "Harri Helsinki"
            (:lahetysaika hakemus) => c/not-nil?))))

    (fact "Hakemuksen lähettäminen - liitteet"
      (asha/with-asha
        (let [id (hc/add-hakemus! hsl-hakemus)
             liite1 {:hakemusid id :nimi "t-1" :contenttype "text/plain"}
             liite2 {:hakemusid id :nimi "t-åäö" :contenttype "text/plain"}]

           (:muokkaaja (hc/get-hakemus+ id)) => nil
           (l/add-liite! liite1 (test/inputstream-from "test-1"))
           (l/add-liite! liite2 (test/inputstream-from "test-2"))
           (:muokkaaja (hc/get-hakemus+ id)) => "Harri Helsinki"

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

           (:diaarinumero (hc/get-hakemus+ id)) => "testing")))

    (fact "Täydennyspyynnön lähettäminen"
      (asha/with-asha
        (let [id (hc/add-hakemus! hsl-hakemus)]

          (h/laheta-hakemus! id)
          (h/taydennyspyynto! id "selite")

          (let [hakemus (hc/get-hakemus+ id)
                taydennyspyynto (:taydennyspyynto hakemus)]
            (:hakemustilatunnus hakemus) => "T0"
            (:selite taydennyspyynto) => "selite"
            (:maarapvm taydennyspyynto) => (test/from-today 14))

          (asha/headers :taydennyspyynto) => asha/valid-headers?
          (:uri (asha/request :taydennyspyynto)) => "/api/hakemus/testing/taydennyspyynto"

          (let [taydennyspyynto (first (test/select-taydennyspyynto {:hakemusid id :nro 1}))
                maarapvm (dbc/date->datetime (:maarapvm taydennyspyynto))
                maarapvm-json (timef/unparse (:date-time timef/formatters) maarapvm)]

            (dbc/clob->string (:selite taydennyspyynto)) =>  "selite"
            (slurp (:body (asha/request :taydennyspyynto))) =>
              (str "{\"maaraaika\":\"" maarapvm-json "\","
                    "\"kasittelija\":\"Harri Helsinki\",\"hakija\":\"Helsingin seudun liikenne\"}")))))

    (fact "Täydennyksen lähettäminen"
      (asha/with-asha
       (let [id (hc/add-hakemus! hsl-hakemus)]

         (h/laheta-hakemus! id)
         (h/taydennyspyynto! id)
         (h/laheta-taydennys! id)

         (:hakemustilatunnus (hc/get-hakemus+ id)) => "TV"

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
       (let [id (hc/add-hakemus! hsl-hakemus)]

         (h/laheta-hakemus! id)
         (h/tarkasta-hakemus! id)

         (:hakemustilatunnus (hc/get-hakemus+ id)) => "T"

         (asha/headers :tarkastettu) => asha/valid-headers?
         (:uri (asha/request :tarkastettu))) => "/api/hakemus/testing/tarkastettu"))

  (fact "Maksatushakemuksen lähettäminen"
    (asha/with-asha
      (let [id1 (hc/add-hakemus! hsl-hakemus)
            id2 (hc/add-hakemus! (assoc hsl-hakemus :hakemustyyppitunnus "MH1"))]

        (h/laheta-hakemus! id1)
        (test/with-user "juku_kasittelija" ["juku_kasittelija"] (do (h/get-hakemus-by-id! id1)))
        (h/laheta-hakemus! id2)

        (:hakemustilatunnus (hc/get-hakemus+ id1)) => "V"
        (:hakemustilatunnus (hc/get-hakemus+ id2)) => "V"

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
            (let [id (hc/add-hakemus! hsl-hakemus)]

              (operation id) =>
              (throws (str "Hakemuksen (" id ") "
                           operationname " ei ole sallittu tilassa: K. Hakemuksen " operationname
                           " on sallittu vain tilassa: "
                           expected-hakemustilatunnus)))))))

(facts "Virheelliset tilasiirtymät"
       (assert-state-transition ["V" "TV"] h/tarkasta-hakemus! "tarkastaminen")
       (assert-state-transition ["V" "TV"] h/taydennyspyynto! "täydennyspyyntö")
       (assert-state-transition ["T0"] h/laheta-taydennys! "täydentäminen"))

(facts "Hakemuksen tilan hallinta - asiahallinta pois päältä"
  (fact "Hakemuksen lähettäminen - asiahallinta on pois päältä"
     (asha/with-asha-off
       (let [id (hc/add-hakemus! hsl-hakemus)]

         (h/laheta-hakemus! id)
         (:diaarinumero (hc/get-hakemus+ id)) => nil))))

(fact "Hakemuksen käsittelijän automaattinen asettaminen"
  (test/with-user "juku_kasittelija" ["juku_kasittelija"]
    (asha/with-asha
      (let [id (hc/add-hakemus! hsl-hakemus)]

        (:kasittelija (h/get-hakemus-by-id! id)) => nil
        (h/laheta-hakemus! id)

        (:kasittelija (h/get-hakemus-by-id! id)) => nil
        (:kasittelija (h/get-hakemus-by-id! id)) => "juku_kasittelija"

        (:hakemustilatunnus (hc/get-hakemus+ id)) => "V"

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

(fact "Hakemusasiakirjan haku onnistuu hakemustilahistoriasta"
  (test/with-user "juku_hakija" ["juku_hakija"]
    (asha/with-asha-off
      (let [id (hc/add-hakemus! hsl-hakemus)
            asiakirja (h/find-hakemus-pdf id)]

        asiakirja => c/not-nil?
        (h/laheta-hakemus! id)
        (h/find-hakemus-pdf id) => c/not-nil?))))