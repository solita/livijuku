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

; *** csv import ***

(defn import-csv [csv] (k/import-kilpailutukset! (csv/parse-csv csv :delimiter \;)))

(fact
  "CSV lataus - yksi kilpailutus"
  (let [nimi (str "nimi" (rand-int 10000))
        text-csv (str "kohdenimi;organisaatioid;liikennointialoituspvm;liikennointipaattymispvm\n"
                      nimi ";1;1.1.1970;1.2.1970")]
    (import-csv text-csv)
    (let [kilpailutus (coll/find-first (coll/eq :kohdenimi nimi) (k/find-kilpailutukset {}))]
      (:liikennointialoituspvm kilpailutus) => (t/local-date 1970 1 1)
      (:liikennointipaattymispvm kilpailutus) => (t/local-date 1970 2 1))))

; *** loading 10000 kilpailutusta ***

(fact
  "CSV lataus - 875 kilpailutusta"
  (let [before-count (count (k/find-kilpailutukset {}))
        header ["organisaatioid" "kohdenimi" "liikennointialoituspvm" "liikennointipaattymispvm"]
        data (for [organisaatioid (range 1 36)
                   aloituspvm (map (c/partial-first-arg t/local-date 1 1) (range 1990 2015))]
               [organisaatioid (str "Linja " (rand-int 10000)) aloituspvm
                (t/plus aloituspvm (t/years (inc (rand-int 10))))])]

    (k/import-kilpailutukset! (cons header data))
    (count (k/find-kilpailutukset {})) => (+ before-count 875)))