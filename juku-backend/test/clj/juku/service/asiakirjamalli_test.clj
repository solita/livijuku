(ns juku.service.asiakirjamalli-test
  (:require [common.core :as c]
            [juku.service.asiakirjamalli :as akmalli]
            [common.collection :as coll]
            [clojure.java.io :as io]
            [clojure.string :as str]))


(defn- find-test-template [asiakirjalajitunnus hakemustyyppitunnus]
  (slurp (io/reader (io/resource
    (str "templates/"
       (case (str/lower-case asiakirjalajitunnus) "h" "hakemus" "p" "paatos")
       "-" (str/lower-case hakemustyyppitunnus)
       ".txt")))))

(defn- add-embedded-template! [asiakirjalajitunnus hakemustyyppitunnus]
  (akmalli/add-asiakirjamalli!
    (assoc (c/bindings->map asiakirjalajitunnus hakemustyyppitunnus)
     :sisalto (find-test-template asiakirjalajitunnus hakemustyyppitunnus)
     :voimaantulovuosi 0
     :organisaatiolajitunnus nil)))

(defn update-test-asiakirjamallit! []
  (doseq [test-asiakirjamalli (filter (coll/eq :voimaantulovuosi 0M) (akmalli/find-all))]
    (akmalli/delete-asiakirjamalli! (:id test-asiakirjamalli)))

  (add-embedded-template! "H" "AH0")
  (add-embedded-template! "H" "MH1")
  (add-embedded-template! "H" "MH2")
  (add-embedded-template! "H" "ELY")

  (add-embedded-template! "P" "AH0")
  (add-embedded-template! "P" "MH1")
  (add-embedded-template! "P" "MH2")
  (add-embedded-template! "P" "ELY"))
