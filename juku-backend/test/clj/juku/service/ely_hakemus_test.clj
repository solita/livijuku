(ns juku.service.ely-hakemus-test
  (:require [midje.sweet :refer :all]
            [clojure.string :as str]
            [common.collection :as c]
            [juku.db.database :refer [db with-transaction]]
            [juku.service.hakemus-core :as hc]
            [juku.service.hakemus :as h]
            [juku.service.ely-hakemus :as ely]
            [juku.service.avustuskohde :as ak]
            [juku.service.test :as test]
            [juku.db.sql :as dml]
            [common.collection :as coll]))

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
  :tulot                      3M
  :kuvaus                     nil
  })

(def ely-perustiedot
  {
    :siirtymaaikasopimukset 1M   ; ELY hakemuksen siirtymäajan sopimukset
    :joukkoliikennetukikunnat 1M ; ELY hakemuksen joukkoliikennetuki kunnille
  })

(fact
  "Määrärahatarpeiden haku"
  (test/with-user "juku_hakija_ely" ["juku_hakija"]
    (let [id (hc/add-hakemus! (ely1-hakemus))]
      (insert-maararahatarve! id (maararahatarve "BS"))
      (ely/find-hakemus-maararahatarpeet id) => [(maararahatarve "BS")])))

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