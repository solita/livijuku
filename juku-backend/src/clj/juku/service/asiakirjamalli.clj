(ns juku.service.asiakirjamalli
  (:require [juku.db.yesql-patch :as sql]
            [clojure.java.io :as io]
            [common.core :as c]
            [clojure.string :as str]
            [juku.db.coerce :as coerce]))

(sql/defqueries "asiakirjamalli.sql")

(defn find-all [] (select-all-asiakirjamallit))

(defn find-asiakirjamalli [vuosi asiakirjalajitunnus hakemustyyppitunnus organisaatiolajitunnus]
  (first (map (comp coerce/clob->reader :sisalto)
           (select-asiakirjamalli-sisalto
             (c/bindings->map vuosi asiakirjalajitunnus
                              hakemustyyppitunnus organisaatiolajitunnus)))))

(defn add-asiakirjamalli! [asiakirjamalli]
  (insert-asiakirjamalli! asiakirjamalli))

(defn- find-embedded-template [vuosi asiakirjalajitunnus hakemustyyppitunnus organisaatiolajitunnus]
  (slurp (io/reader (io/resource
                      (str "pdf-sisalto/templates/"
                           (case (str/lower-case asiakirjalajitunnus) "h" "hakemus" "p" "paatos")
                           (or (some->> hakemustyyppitunnus str/lower-case (str "-")) "")
                           (or (some->> organisaatiolajitunnus str/lower-case (str "-")) "")
                           "-" vuosi ".txt")))))

(defn- add-embedded-template [voimaantulovuosi asiakirjalajitunnus hakemustyyppitunnus organisaatiolajitunnus]
  (add-asiakirjamalli!
    (assoc (c/bindings->map voimaantulovuosi asiakirjalajitunnus
                            hakemustyyppitunnus organisaatiolajitunnus)
      :sisalto (find-embedded-template voimaantulovuosi asiakirjalajitunnus
                                       hakemustyyppitunnus organisaatiolajitunnus))))

(when (empty? (find-all-asiakirjamallit))
  (add-embedded-template 2016 "H" "AH0" nil))