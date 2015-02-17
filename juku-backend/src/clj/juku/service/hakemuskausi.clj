(ns juku.service.hakemuskausi
  (:require [juku.service.organisaatio :as organisaatio]
            [juku.service.hakemus :as hakemus]
            [juku.schema.hakemuskausi :as s]
            [juku.db.database :refer [db]]
            [clojure.java.jdbc :as jdbc]
            [juku.db.sql :as dml]
            [juku.db.coerce :as coerce]
            [schema.coerce :as scoerce]
            [yesql.core :as sql]
            [common.collection :as c]
            [ring.util.http-response :as r]
            [clj-time.core :as time])
  (:import (java.sql Blob)))

(sql/defqueries "hakemuskausi.sql" {:connection db})

(def coerce-maararaha (scoerce/coercer s/Maararaha coerce/db-coercion-matcher))

(def constraint-errors
  {:hakemuskausi_pk {:http-response r/bad-request :message "Hakemuskausi on jo avattu vuodelle: {vuosi}"}})

(defn find-hakemuskaudet []
  (let [hakemuskaudet (map (fn [kausi] (update-in kausi [:vuosi] int)) (select-all-hakemuskaudet))
        hakemukset (hakemus/find-all-hakemukset)]
    (c/assoc-left-join :hakemukset hakemuskaudet hakemukset :vuosi)))

(defn save-hakuohje [vuosi nimi content-type ^java.io.InputStream hakuohje]
  (jdbc/with-db-transaction [db-spec db]
    (merge-hakemuskausi-hakuohje! {:vuosi vuosi :nimi nimi :contenttype content-type})
    (update-hakemuskausi-set-hakuohje-sisalto! {:vuosi vuosi :sisalto hakuohje})
    nil))

(defn find-hakuohje-sisalto [vuosi]
  (if-let [ohje (first (select-hakuohje-sisalto {:vuosi vuosi}))]
    (update-in ohje [:sisalto] #(.getBinaryStream ^Blob %))))

(defn find-maararaha [vuosi organisaatiolajitunnus]
  (first (map coerce-maararaha (select-maararaha {:vuosi vuosi :organisaatiolajitunnus organisaatiolajitunnus}))))

(defn- update-maararaha! [maararaha]
  (dml/update-where! db "maararaha"
                     (dissoc maararaha :vuosi :organisaatiolajitunnus)
                     (select-keys maararaha [:vuosi :organisaatiolajitunnus])))

(defn- insert-maararaha! [maararaha]
  (dml/insert db "maararaha" maararaha constraint-errors maararaha))

(defn save-maararaha! [maararaha]
  (if (= (update-maararaha! maararaha) 0) (insert-maararaha! maararaha)))

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
    (dml/assert-update (update-hakemuskausi-set-tila! {:vuosi vuosi :newtunnus "K" :expectedtunnus "A"})
      (if (empty? (select-hakemuskausi {:vuosi vuosi}))
        {:http-response r/not-found :message (str "Hakemuskautta ei ole olemassa vuodelle: " vuosi) :vuosi vuosi}
        {:http-response r/method-not-allowed :message (str "Hakemuskausi on jo avattu vuodelle: " vuosi) :vuosi vuosi}))
    (doseq [organisaatio (organisaatio/hakija-organisaatiot)]
      (hakemus/add-hakemus! (oletus-avustus-hakemus! vuosi (:id organisaatio)))
      (hakemus/add-hakemus! (oletus-maksatus-hakemus1! vuosi (:id organisaatio)))
      (hakemus/add-hakemus! (oletus-maksatus-hakemus2! vuosi (:id organisaatio))))))


