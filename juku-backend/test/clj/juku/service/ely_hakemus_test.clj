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
            [juku.db.sql :as dml]))

(defn- insert-maararahatarve! [hakemusid maararahatarve]
  (:id (dml/insert db "maararahatarve" (assoc maararahatarve :hakemusid hakemusid) ely/maararahatarve-constraint-errors maararahatarve)))

(def hakemuskausi (test/next-avattu-empty-hakemuskausi!))
(def vuosi (:vuosi hakemuskausi))
(def ely1-hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid 16M})

(defn maararahatarve [tyyppi]
  {
  :maararahatarvetyyppitunnus tyyppi,
  :sidotut                    1M
  :uudet                      2M
  :tulot                      3M
  :kuvaus                     nil
  })

(fact
  "Määrärahatarpeiden haku"
  (test/with-user "juku_hakija_ely" ["juku_hakija"]
    (let [id (hc/add-hakemus! ely1-hakemus)]
      (insert-maararahatarve! id (maararahatarve "BS"))
      (ely/find-hakemus-maararahatarpeet id) => [(maararahatarve "BS")])))

(fact
  "Määrärahatarpeiden haku - kaksi tarvetta"
  (test/with-user "juku_hakija_ely" ["juku_hakija"]
    (let [id (hc/add-hakemus! ely1-hakemus)
          bs (maararahatarve "BS")
          kk1 (maararahatarve "KK1")]
      (insert-maararahatarve! id bs)
      (insert-maararahatarve! id kk1)
      (ely/find-hakemus-maararahatarpeet id) => [bs kk1])))

(fact
  "Määrärahatarpeen päivitys"
  (test/with-user "juku_hakija_ely" ["juku_hakija"]
    (let [id (hc/add-hakemus! ely1-hakemus)
          bs (maararahatarve "BS")
          new (assoc bs :sidotut 3M)]
      (insert-maararahatarve! id bs)
      (ely/find-hakemus-maararahatarpeet id) => [bs]

      (ely/save-maararahatarpeet! id [new])
      (ely/find-hakemus-maararahatarpeet id) => [new])))

(fact
  "Määrärahatarpeen päivitys - useampi määrärahatarve"
  (test/with-user "juku_hakija_ely" ["juku_hakija"]
    (let [id (hc/add-hakemus! ely1-hakemus)
          bs (maararahatarve "BS")
          kk1 (maararahatarve "KK1")
          new (assoc bs :sidotut 3M)]
      (insert-maararahatarve! id bs)
      (insert-maararahatarve! id kk1)
      (ely/find-hakemus-maararahatarpeet id) => [bs kk1]

      (ely/save-maararahatarpeet! id [new])
      (ely/find-hakemus-maararahatarpeet id) => [new kk1])))


