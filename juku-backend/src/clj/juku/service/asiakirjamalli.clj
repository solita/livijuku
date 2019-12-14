(ns juku.service.asiakirjamalli
  (:require [juku.db.yesql-patch :as sql]
            [clojure.java.io :as io]
            [common.core :as c]
            [ring.util.http-response :as r]
            [clojure.string :as str]
            [schema.coerce :as scoerce]
            [juku.db.coerce :as coerce]
            [juku.schema.asiakirjamalli :as s]))

(def constraint-errors
  {:asiakirjamalli_u {:http-response r/conflict :message "Asiakirjamalli on jo olemassa"}
   :asiakirjamalli_hatyyppi_fk {:http-response r/not-found :message "Hakemustyyppiä: {hakemustyyppitunnus} ei ole olemasssa."}})


(sql/defqueries "asiakirjamalli.sql" {:constraint-errors constraint-errors
                                      :dissoc-error-params [:sisalto]})

(def coerce-asiakirja (scoerce/coercer s/Asiakirjamalli coerce/db-coercion-matcher))
(def coerce-asiakirja+ (scoerce/coercer s/Asiakirjamalli+sisalto coerce/db-coercion-matcher))

(defn find-all [] (map coerce-asiakirja (select-all-asiakirjamallit)))

(defn find-by-id [id] (first (map coerce-asiakirja+ (select-asiakirjamalli-by-id {:id id}))))

(defn find-asiakirjamalli [vuosi asiakirjalajitunnus hakemustyyppitunnus organisaatiolajitunnus]
  (first (map (comp coerce/clob->reader :sisalto)
           (select-asiakirjamalli-sisalto
             (c/bindings->map vuosi asiakirjalajitunnus
                              hakemustyyppitunnus organisaatiolajitunnus)))))

(defn edit-asiakirjamalli! [id asiakirjamalli]
  (update-asiakirjamalli! (assoc asiakirjamalli :id id)))

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

(when (empty? (find-all))
  (add-embedded-template 2016 "H" "AH0" nil))