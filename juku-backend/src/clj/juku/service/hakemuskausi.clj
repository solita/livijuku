(ns juku.service.hakemuskausi
  (:require [juku.service.organisaatio :as organisaatio]
            [juku.service.hakemus :as hakemus]
            [juku.db.database :refer [db]]
            [juku.db.sql :as dml]
            [yesql.core :as sql]
            [clojure.java.jdbc :as jdbc]
            [ring.util.http-response :as r]
            [clj-time.core :as time])
  (:import (java.sql Blob)))

(sql/defqueries "hakemuskausi.sql" {:connection db})

(def constraint-errors
  {:hakemuskausi_pk {:http-response r/bad-request :message "Hakemuskausi on jo avattu vuodelle: {vuosi}"}})

(defn save-hakuohje [vuosi nimi content-type ^java.io.InputStream hakuohje]
  (jdbc/with-db-transaction [db-spec db]
    (merge-hakemuskausi-hakuohje! {:vuosi vuosi :nimi nimi :contenttype content-type})
    (update-hakemuskausi-set-hakuohje-sisalto! {:vuosi vuosi :sisalto hakuohje})))

(defn find-hakuohje-sisalto [vuosi]
  (if-let [ohje (first (select-hakuohje-sisalto {:vuosi vuosi}))]
    (update-in ohje [:sisalto] #(.getBinaryStream ^Blob %))))

(defn add-hakemuskausi! [vuosi]
  (let [hakemuskausi {:vuosi vuosi}]
    (dml/insert db "hakemuskausi" hakemuskausi constraint-errors hakemuskausi)))

(defn- oletus-avustus-hakemus! [vuosi organisaatioid] {
     :vuosi vuosi :hakemustyyppitunnus "AH0"
     :organisaatioid organisaatioid
     :hakuaika {:alkupvm (time/local-date (- vuosi 1) 9 1)
                :loppupvm (time/local-date (- vuosi 1) 12 15)}})

(defn- oletus-maksatus-hakemus1! [vuosi organisaatioid] {
     :vuosi vuosi :hakemustyyppitunnus "MH1"
     :organisaatioid organisaatioid
     :hakuaika {:alkupvm (time/local-date vuosi 7 1)
                :loppupvm (time/local-date vuosi 8 31)}})

(defn- oletus-maksatus-hakemus2! [vuosi organisaatioid] {
       :vuosi vuosi :hakemustyyppitunnus "MH2"
       :organisaatioid organisaatioid
       :hakuaika {:alkupvm (time/local-date (+ vuosi 1) 1 1)
                  :loppupvm (time/local-date (+ vuosi 1) 1 31)}})

(defn avaa-hakemuskausi! [vuosi]
  (jdbc/with-db-transaction [db-spec db]
    (doseq [organisaatio (organisaatio/hakija-organisaatiot)]
      (hakemus/add-hakemus! (oletus-avustus-hakemus! vuosi (:id organisaatio)))
      (hakemus/add-hakemus! (oletus-maksatus-hakemus1! vuosi (:id organisaatio)))
      (hakemus/add-hakemus! (oletus-maksatus-hakemus2! vuosi (:id organisaatio))))))


