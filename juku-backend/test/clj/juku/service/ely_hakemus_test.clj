(ns juku.service.ely-hakemus-test
  (:require [midje.sweet :refer :all]
            [clojure.string :as str]
            [common.collection :as coll]
            [juku.db.database :refer [db with-transaction]]
            [juku.service.hakemus-core :as hc]
            [juku.service.hakemus :as h]
            [juku.service.ely-hakemus :as ely]
            [juku.service.avustuskohde :as ak]
            [juku.service.test :as test]
            [juku.db.sql :as dml]
            [juku.service.asiahallinta-mock :as asha]
            [juku.service.pdf-mock :as pdf]
            [juku.service.liitteet :as l]
            [juku.headers :as headers]
            [common.string :as strx]
            [juku.service.paatos :as p]
            [common.core :as c]))

(defn- insert-maararahatarve! [hakemusid maararahatarve]
  (:id (dml/insert db "maararahatarve" (assoc maararahatarve :hakemusid hakemusid) ely/maararahatarve-constraint-errors maararahatarve)))

(def hakemuskausi (test/next-avattu-empty-hakemuskausi!))
(def vuosi (:vuosi hakemuskausi))

(defn ely1-hakemus
  ([] (ely1-hakemus vuosi))
  ([vuosi] {:vuosi vuosi :hakemustyyppitunnus "ELY" :organisaatioid 16M}))

(defn maararahatarve [tyyppi]
  {
  :maararahatarvetyyppitunnus tyyppi,
  :sidotut                    1M
  :uudet                      2M
  :kuvaus                     nil
  })

(defn maararahatarve-bs [tulot] (assoc (maararahatarve "BS") :tulot tulot))

(def ely-perustiedot
  {
    :siirtymaaikasopimukset 1M   ; ELY hakemuksen siirtymäajan sopimukset
    :joukkoliikennetukikunnat 1M ; ELY hakemuksen joukkoliikennetuki kunnille
  })

(fact
  "Määrärahatarpeiden haku"
  (test/with-user "juku_hakija_ely" ["juku_hakija"]
    (let [id (hc/add-hakemus! (ely1-hakemus))]
      (insert-maararahatarve! id (maararahatarve-bs 2M))
      (ely/find-hakemus-maararahatarpeet id) => [(maararahatarve-bs 2M)])))

(fact
  "Määrärahatarpeiden haku - ei tuloa"
  (test/with-user "juku_hakija_ely" ["juku_hakija"]
    (let [id (hc/add-hakemus! (ely1-hakemus))]
      (insert-maararahatarve! id (maararahatarve "KK1"))
      (ely/find-hakemus-maararahatarpeet id) => [(maararahatarve "KK1")])))

(fact
  "Määrärahatarpeiden haku - kaksi tarvetta"
  (test/with-user "juku_hakija_ely" ["juku_hakija"]
    (let [id (hc/add-hakemus! (ely1-hakemus))
          bs (maararahatarve "BS")
          kk1 (maararahatarve "KK1")]
      (insert-maararahatarve! id bs)
      (insert-maararahatarve! id kk1)
      (ely/find-hakemus-maararahatarpeet id) => [bs kk1])))

(fact
  "Määrärahatarpeen päivitys"
  (test/with-user "juku_hakija_ely" ["juku_hakija"]
    (let [id (hc/add-hakemus! (ely1-hakemus))
          bs (maararahatarve "BS")
          new (assoc bs :sidotut 3M)]
      (insert-maararahatarve! id bs)
      (ely/find-hakemus-maararahatarpeet id) => [bs]

      (ely/save-maararahatarpeet! id [new])
      (ely/find-hakemus-maararahatarpeet id) => [new])))

(fact
  "Määrärahatarpeen päivitys - useampi määrärahatarve"
  (test/with-user "juku_hakija_ely" ["juku_hakija"]
    (let [id (hc/add-hakemus! (ely1-hakemus))
          bs (maararahatarve "BS")
          kk1 (maararahatarve "KK1")
          new (assoc bs :sidotut 3M)]
      (insert-maararahatarve! id bs)
      (insert-maararahatarve! id kk1)
      (ely/find-hakemus-maararahatarpeet id) => [bs kk1]

      (ely/save-maararahatarpeet! id [new])
      (ely/find-hakemus-maararahatarpeet id) => [new kk1])))

(fact
  "Kehityshankkeiden haku"
  (test/with-user "juku_hakija_ely" ["juku_hakija"]
    (let [id (hc/add-hakemus! (ely1-hakemus))
          kehityshanke {:numero 1M :nimi "testi" :arvo 1M :kuvaus "asdf"}]
      (ely/save-kehityshankkeet! id [kehityshanke])
      (ely/find-hakemus-kehityshankkeet id) => [kehityshanke])))


(fact
  "ELY hakemuksen perustietojen päivitys ja haku"
  (test/with-user "juku_hakija_ely" ["juku_hakija"]
    (let [hk (test/next-avattu-empty-hakemuskausi!)
          h (ely1-hakemus (:vuosi hk))
          id (hc/add-hakemus! h)]


      (ely/save-elyhakemus id ely-perustiedot)
      (dissoc (hc/get-hakemus+ id) :muokkausaika)
        => (assoc h :ely ely-perustiedot
                    :selite nil
                    :other-hakemukset []
                    :hakuaika (:hakuaika (coll/find-first (coll/eq :hakemustyyppitunnus "ELY") (:hakemukset hk)))
                    :luontitunnus "juku_hakija_ely"
                    :kasittelijanimi nil,
                    :tilinumero nil,
                    :lahettaja nil
                    :muokkaaja nil
                    :hakemustilatunnus "K"
                    :kasittelija nil
                    :diaarinumero nil
                    :lahetysaika nil
                    :contentvisible true
                    :id id))))

(fact
  "ELY hakemussuunnitelma haku"
  (test/with-user "juku_hakija_ely" ["juku_hakija"]
    (let [h (ely1-hakemus)
          id (hc/add-hakemus! h)]

      (insert-maararahatarve! id (assoc (maararahatarve "BS") :tulot 0))
      (insert-maararahatarve! id (assoc (maararahatarve "KK1") :tulot 1))
      (coll/find-first (coll/eq :id id) (hc/find-hakemussuunnitelmat vuosi "ELY"))
        => (assoc h
             :id id
             :hakuaika (:hakuaika (coll/find-first (coll/eq :hakemustyyppitunnus "ELY") (:hakemukset hakemuskausi)))
             :diaarinumero nil
             :hakemustilatunnus "K"
             :myonnettava-avustus 0M
             :haettu-avustus 5M))))

(fact
  "ELY hakemussuunnitelma haku - ely-hakemuksen perustiedot - LIVIJUKU-622"
  (test/with-user "juku_hakija_ely" ["juku_hakija"]
    (let [h (ely1-hakemus)
          id (hc/add-hakemus! h)]

      (ely/save-elyhakemus id ely-perustiedot)
      (insert-maararahatarve! id (assoc (maararahatarve "BS") :tulot 2))
      (insert-maararahatarve! id (maararahatarve "KK1"))

      (coll/find-first (coll/eq :id id) (hc/find-hakemussuunnitelmat vuosi "ELY"))
      => (assoc h
           :id id
           :hakuaika (:hakuaika (coll/find-first (coll/eq :hakemustyyppitunnus "ELY") (:hakemukset hakemuskausi)))
           :diaarinumero nil
           :hakemustilatunnus "K"
           :myonnettava-avustus 0M
           :haettu-avustus 6M))))

(defn assert-elyhakemus-pdf [content]
  (fact "tarkasta ely-hakemus"
        content => (partial strx/substring? (str "Hakemus " pdf/today))
        content => (partial strx/substring? "ELY-keskus: Uusimaa")
        content => #"Sidotut kustannukset\s+1 e"
        content => #"Uudet sopimukset\s+2 e"
        content => #"Kauden tulot\s+1 e"
        content => #"testi1234\s+1 e"
        content => #"Määrärahatarpeet yhteensä\s+11 e"))

(fact "ELY-hakemuksen lähettäminen"
  (test/with-user "juku_hakija_ely" ["juku_hakija"]
    (asha/with-asha
      (let [id (hc/add-hakemus! (ely1-hakemus))
            liite1 {:hakemusid id :nimi "test" :contenttype "text/plain"}]

        (:muokkaaja (hc/get-hakemus+ id)) => nil

        (insert-maararahatarve! id (maararahatarve-bs 1))
        (insert-maararahatarve! id (maararahatarve "KK1"))
        (insert-maararahatarve! id (maararahatarve "KK2"))

        (ely/save-kehityshankkeet! id [{:numero 1M :nimi "testi1234" :arvo 1M :kuvaus "asdf"}])

        (ely/save-elyhakemus id ely-perustiedot)

        (l/add-liite! liite1 (test/inputstream-from "test"))

        (:muokkaaja (hc/get-hakemus+ id)) => "Elli Ely"

        (h/laheta-hakemus! id)
        (asha/headers :vireille) => asha/valid-headers?

        (let [request (asha/request :vireille)
              multipart (asha/group-by-multiparts request)
              hakemus-asiakirja (get multipart "hakemus-asiakirja")]

          (get-in multipart ["hakemus" :content]) =>
          (str "{\"kausi\":\"test/" vuosi "\","
                "\"tyyppi\":\"ELY\","
                "\"hakija\":\"Uusimaa\","
                "\"omistavaOrganisaatio\":\"Liikennevirasto\","
                "\"omistavaHenkilo\":\"test\"}")

          (:mime-type hakemus-asiakirja) => "application/pdf"
          (:part-name hakemus-asiakirja) => "hakemus-asiakirja"
          (:name hakemus-asiakirja) => "hakemus.pdf"

          (assert-elyhakemus-pdf (pdf/pdf->text (:content hakemus-asiakirja)))

          (slurp (get-in multipart ["test" :content])) => "test")

        (:diaarinumero (hc/get-hakemus+ id)) => "testing"

        (assert-elyhakemus-pdf (pdf/pdf->text (h/find-hakemus-pdf id)))))))

(fact "ELY-päätöksen hyväksyminen"
  (test/with-user "juku_kasittelija" ["juku_kasittelija"]
    (asha/with-asha
      (let [hk (test/next-avattu-empty-hakemuskausi!)
            vuosi (:vuosi hk)
            id (hc/add-hakemus! (ely1-hakemus vuosi))
            paatos {:hakemusid id, :myonnettyavustus 1M :selite "FooBar"}]

        (p/save-paatos! paatos)

        (test/with-user "juku_hakija_ely" ["juku_hakija"] (h/laheta-hakemus! id))
        (h/tarkasta-hakemus! id)
        (p/hyvaksy-paatokset! vuosi "ely")

        (let [request (asha/request :paatos)
              multipart (asha/group-by-multiparts request)
              paatos-asiakirja (get multipart "paatos-asiakirja")]

          (:uri request) => "/api/hakemus/testing/paatos"

          (get-in multipart ["paatos" :content]) => "{\"paattaja\":\"Katri Käsittelijä\"}"

          (:mime-type paatos-asiakirja) => "application/pdf"
          (:part-name paatos-asiakirja) => "paatos-asiakirja"
          (:name paatos-asiakirja) => "paatos.pdf")

        (:hakemustilatunnus (hc/get-hakemus+ id)) => "P"

        (let [hyvaksytty-paatos (p/find-current-paatos id)]
          (:paattaja hyvaksytty-paatos) => "juku_kasittelija"
          (:voimaantuloaika hyvaksytty-paatos) => c/not-nil?
          (:poistoaika hyvaksytty-paatos) => nil)))))