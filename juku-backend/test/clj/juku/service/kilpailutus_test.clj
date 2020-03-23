(ns juku.service.kilpailutus-test
  (:require [midje.sweet :refer :all]
            [juku.service.test :as test]
            [juku.service.kilpailutus :as k]
            [juku.schema.kilpailutus :as ks]
            [common.map :as map]
            [clojure.string :as str]
            [common.collection :as coll]
            [clojure-csv.core :as csv]
            [common.string :as strx]
            [clj-time.core :as t]
            [common.core :as c]))

(fact
  "Kilpailutuksen tallennus ja yksittäisen kilpailutuksen haku"
  (test/with-hakija
    (let [kilpailutus {:organisaatioid 1M
                       :kohdenimi "test"
                       :liikennointialoituspvm    (t/local-date 2016 1 1)
                       :liikennointipaattymispvm  (t/local-date 2016 2 1)
                       :tarjousmaara  1M
                       :tarjoushinta1 1M
                       :tarjoushinta2 1M}
          id (k/add-kilpailutus! kilpailutus)]
      (k/get-kilpailutus! id) => (merge (map/map-values (constantly nil) ks/Kilpailutus)
                                        (assoc kilpailutus :id id))
      (test/with-public-user
        (k/get-kilpailutus! id) => (merge (map/map-values (constantly nil) ks/Kilpailutus)
                                          (assoc kilpailutus :id id
                                                             :tarjousmaara  nil
                                                             :tarjoushinta1 nil
                                                             :tarjoushinta2 nil))))))

(fact
  "Kilpailutuksen tallennus, muokkaus ja yksittäisen kilpailutuksen haku"
  (test/with-hakija
    (let [kilpailutus {:organisaatioid 1M
                       :kohdenimi "test"
                       :liikennointialoituspvm    (t/local-date 2016 1 1)
                       :liikennointipaattymispvm  (t/local-date 2016 2 1)
                       :tarjousmaara  1M
                       :tarjoushinta1 1M
                       :tarjoushinta2 1M}
          edit-kilpailutus (assoc kilpailutus :tarjousmaara 2M)
          id (k/add-kilpailutus! kilpailutus)]

      (k/edit-kilpailutus! id edit-kilpailutus)
      (k/get-kilpailutus! id) => (merge (map/map-values (constantly nil) ks/Kilpailutus)
                                        (assoc edit-kilpailutus :id id)))))

(fact
  "Kilpailutuksen tallennus, muokkaus - väärä organisaatio id"
  (test/with-user "juku_kasittelija" ["juku_kasittelija"]
    (let [kilpailutus {:organisaatioid 1M
                       :kohdenimi "test"
                       :liikennointialoituspvm    (t/local-date 2016 1 1)
                       :liikennointipaattymispvm  (t/local-date 2016 2 1)
                       :tarjousmaara  1M
                       :tarjoushinta1 1M
                       :tarjoushinta2 1M}
          edit-kilpailutus (assoc kilpailutus :organisaatioid 6666666M)
          id (k/add-kilpailutus! kilpailutus)]

      (k/edit-kilpailutus! id edit-kilpailutus) =>
        (throws "Kilpailutuksen organisaatiota 6666666 ei ole olemassa."))))

(fact
  "Kilpailutuksen tallennus ja kaikkien kilpailutusten haku"
  (test/with-hakija
    (let [kilpailutus {:organisaatioid 1M
                       :kohdenimi "test"
                       :liikennointialoituspvm    (t/local-date 2016 1 1)
                       :liikennointipaattymispvm  (t/local-date 2016 2 1)
                       :tarjousmaara  1M
                       :tarjoushinta1 1M
                       :tarjoushinta2 1M}
          id (k/add-kilpailutus! kilpailutus)
          find-kilpailutus (fn [] (coll/find-first (coll/eq :id id) (k/find-kilpailutukset {})))]
      (find-kilpailutus) => (merge (map/map-values (constantly nil) ks/Kilpailutus)
                                   (assoc kilpailutus :id id))
      (test/with-public-user
        (find-kilpailutus) => (merge (map/map-values (constantly nil) ks/Kilpailutus)
                                     (assoc kilpailutus :id id
                                                        :tarjousmaara  nil
                                                        :tarjoushinta1 nil
                                                        :tarjoushinta2 nil))))))

; *** csv import ***

(defn import-csv [csv] (k/import-kilpailutukset! (csv/parse-csv csv :delimiter \;)))

(fact
  "CSV lataus - yksi kilpailutus"
  (test/with-hakija
    (let [nimi (str "nimi" (rand-int 10000))
          text-csv (str "kohdenimi;organisaatioid;liikennointialoituspvm;liikennointipaattymispvm\n"
                        nimi ";1;1.1.1970;1.2.1970")]
      (import-csv text-csv)
      (let [kilpailutus (coll/find-first (coll/eq :kohdenimi nimi) (k/find-kilpailutukset {}))]
        (:liikennointialoituspvm kilpailutus) => (t/local-date 1970 1 1)
        (:liikennointipaattymispvm kilpailutus) => (t/local-date 1970 2 1)))))

; *** loading 875 kilpailutusta ***

(fact
  "CSV lataus - 875 kilpailutusta"
  (test/with-user "juku_paatoksentekija" ["juku_paatoksentekija"]
    (let [before-count (count (k/find-kilpailutukset {}))
          header ["organisaatioid" "kohdenimi" "liikennointialoituspvm" "liikennointipaattymispvm"]
          data (for [organisaatioid (range 1 36)
                     aloituspvm (map (c/partial-first-arg t/local-date 1 1) (range 1990 2015))]
                 [organisaatioid (str "Linja " (rand-int 10000)) aloituspvm
                  (t/plus aloituspvm (t/years (inc (rand-int 10))))])]

      (k/import-kilpailutukset! (cons header data))
      (count (k/find-kilpailutukset {})) => (+ before-count 875))))