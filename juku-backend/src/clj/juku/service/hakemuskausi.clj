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
  (:import (java.sql Blob)
           (java.io InputStream)))

(sql/defqueries "hakemuskausi.sql" {:connection db})

(def coerce-maararaha (scoerce/coercer s/Maararaha coerce/db-coercion-matcher))

(def constraint-errors
  {:hakemuskausi_pk {:http-response r/bad-request :message "Hakemuskausi on jo avattu vuodelle: {vuosi}"}})

(defn- oletushakemuskausi [vuosi] {:vuosi vuosi :tilatunnus "0" :hakuohje_contenttype nil})

(defn- find-all-hakemuskaudet+seuraava-kausi [init-hakemuskausi]
  (let [hakemuskaudet (map (fn [kausi] (update-in kausi [:vuosi] int)) (select-all-hakemuskaudet))
        nextvuosi (+ (time/year (time/now)) 1)]
    (if (some (c/eq :vuosi nextvuosi) hakemuskaudet)
      hakemuskaudet
      (conj hakemuskaudet (init-hakemuskausi nextvuosi)))))

(defn find-hakemuskaudet+hakemukset []
  (let [init-hakemuskausi (fn [vuosi] (assoc (oletushakemuskausi vuosi) :hakemukset []))
        hakemuskaudet (find-all-hakemuskaudet+seuraava-kausi init-hakemuskausi)
        hakemukset (hakemus/find-all-hakemukset)]
    (c/assoc-left-join hakemuskaudet :hakemukset hakemukset :vuosi)))

#_(defn find-hakemuskaudet+summary []
  (let [init-hakemuskausi (fn [vuosi] (assoc (oletushakemuskausi vuosi) :hakemukset []))
        hakemuskaudet (find-all-hakemuskaudet+seuraava-kausi init-hakemuskausi)
        hakemukset (select-all-hakuajat)
        hakuajat (count-hakemustilat-for-vuosi-hakemustyyppi)
        hakemukset+hakuajat (c/assoc-left-join hakemukset :hakuajat hakuajat :vuosi :hakemustyyppitunnus)]

    (c/assoc-left-join hakemuskaudet :hakemukset hakemukset+hakuajat :vuosi :hakemustyyppitunnus)))

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

(defn- oletus-avustus-hakuaika [vuosi] {
     :vuosi vuosi :hakemustyyppitunnus "AH0"
     :alkupvm (time/local-date (- vuosi 1) 9 1)
     :loppupvm (time/local-date (- vuosi 1) 12 15)})

(defn- oletus-maksatus-hakuaika1 [vuosi] {
     :vuosi vuosi :hakemustyyppitunnus "MH1"
     :alkupvm (time/local-date vuosi 7 1)
     :loppupvm (time/local-date vuosi 8 31)})

(defn- oletus-maksatus-hakuaika2 [vuosi] {
       :vuosi vuosi :hakemustyyppitunnus "MH2"
       :alkupvm (time/local-date (+ vuosi 1) 1 1)
       :loppupvm (time/local-date (+ vuosi 1) 1 31)})

(defn- insert-hakuaika! [hakuaika]
  (dml/insert db "hakuaika" (coerce/localdate->sql-date hakuaika) constraint-errors hakuaika))

(defn- insert-hakemuskauden-oletus-hakuajat! [vuosi]
  (let [assoc-vuosi (fn [m] (assoc m :vuosi vuosi))])
  (insert-hakuaika! (oletus-avustus-hakuaika vuosi))
  (insert-hakuaika! (oletus-maksatus-hakuaika1 vuosi))
  (insert-hakuaika! (oletus-maksatus-hakuaika2 vuosi)))

(defn init-hakemuskausi! [vuosi]
  (if (= (insert-hakemuskausi-if-not-exists! {:vuosi vuosi}) 1)
    (insert-hakemuskauden-oletus-hakuajat! vuosi) 0))

(defn- update-hakuaika! [hakuaika]
  (dml/update-where! db "hakuaika"
     (dissoc hakuaika :vuosi :hakemustyyppitunnus)
     (select-keys hakuaika [:vuosi :hakemustyyppitunnus])))

(defn save-hakemuskauden-hakuajat! [vuosi hakuajat]
  (jdbc/with-db-transaction [db-spec db]
    (init-hakemuskausi! vuosi)
    (doseq [hakuaika hakuajat] (update-hakuaika! (assoc hakuaika :vuosi vuosi)))
    nil))

(defn save-hakuohje [vuosi nimi content-type ^InputStream hakuohje]
  (jdbc/with-db-transaction [db-spec db]
    (init-hakemuskausi! vuosi)
    (update-hakemuskausi-set-hakuohje! {:vuosi vuosi :nimi nimi :contenttype content-type :sisalto hakuohje})
    nil))

(defn avaa-hakemuskausi! [vuosi]
  (jdbc/with-db-transaction [db-spec db]
    (init-hakemuskausi! vuosi)
    (dml/assert-update (update-hakemuskausi-set-tila! {:vuosi vuosi :newtunnus "K" :expectedtunnus "A"})
      {:http-response r/method-not-allowed :message (str "Hakemuskausi on jo avattu vuodelle: " vuosi) :vuosi vuosi})
    (doseq [organisaatio (organisaatio/hakija-organisaatiot)]
      (let [hakemus (fn [hakemustyyppitunnus] {:vuosi vuosi :hakemustyyppitunnus hakemustyyppitunnus :organisaatioid (:id organisaatio)})]
        (hakemus/add-hakemus! (hakemus "AH0"))
        (hakemus/add-hakemus! (hakemus "MH1"))
        (hakemus/add-hakemus! (hakemus "MH2"))))))


