(ns juku.service.kilpailutus
  (:require [juku.db.yesql-patch :as sql]
            [juku.db.database :refer [db with-transaction]]
            [juku.db.coerce :as coerce]
            [juku.schema.common :as js]
            [juku.schema.kilpailutus :as s]
            [ring.util.http-response :as r]
            [juku.db.sql :as dml]
            [common.core :as c]
            [honeysql.core :as hsql]
            [juku.db.sql :as jsql]
            [common.collection :as coll]
            [clojure.string :as str]
            [clj-time.format :as f]
            [schema.core :as sc]
            [schema.coerce :as sc-coerce]
            [common.map :as m]
            [common.string :as strx]
            [juku.service.user :as user]
            [slingshot.slingshot :as ss])
  (:import (org.joda.time LocalDate)))

; *** Kilpailutuksiin liittyvät kyselyt ***
(sql/defqueries "kilpailutus.sql")

; *** Kilpailutus-skeemaan liittyvät konversiot tietokannan tietotyypeistä ***
(def coerce-kilpailutus (coerce/coercer s/Kilpailutus))
(def coerce-luokka (coerce/coercer js/Luokka))

; *** Virheviestit tietokannan rajoitteista ***
(def constraint-errors {
   :kilpailutus_organisaatio_fk {:http-response r/not-found :message "Kilpailutuksen organisaatiota {organisaatioid} ei ole olemassa."}})

(derive ::kilpailutus-not-found ::coll/not-found)

(defn find-sopimusmallit [] (map coerce-luokka (select-sopimusmallit)))

(defn find-kilpailutus [kilpailutusid]
  (first (map coerce-kilpailutus (select-kilpailutus (c/bindings->map kilpailutusid)))))

(defn get-kilpailutus! [kilpailutusid]
  (c/assert-not-nil! (find-kilpailutus kilpailutusid)
                     {:type ::kilpailutus-not-found
                      :message (str "Kilpailutusta " kilpailutusid " ei ole olemassa. ")
                      :kilpailutusid kilpailutusid}))

(defn forbidden! [msg] (ss/throw+ {:http-response r/forbidden :message msg} msg))

(defn assert-modify-kilpailutus-allowed*!
  "Kilpailutusta voi muokata vain jos:
   - käyttäjällä on oikeus muokata kaikkia kilpailutuksia tai
   - käyttäjä on kilpailutuksen omistaja ja käyttäjällä on omien kilpailutusten hallintaoikeus"

  [kilpailutus]
  (when-not (or (user/has-privilege* :modify-kaikki-kilpailutukset)
                (and (user/has-privilege* :modify-omat-kilpailutukset)
                     (= (:organisaatioid user/*current-user*) (:organisaatioid kilpailutus))))
    (if-let [id (:id kilpailutus)]
      (forbidden!
        (str "Käyttäjällä " (:tunnus user/*current-user*)
             " ei ole oikeutta muokata tai poistaa kilpailutusta: " id "."))
      (forbidden!
        (str "Käyttäjällä " (:tunnus user/*current-user*)
             " ei ole oikeutta lisätä kilpailutuksia.")))))

(defn add-kilpailutus! [kilpailutus]
  (assert-modify-kilpailutus-allowed*! kilpailutus)
  (:id (dml/insert-with-id db "kilpailutus" kilpailutus constraint-errors kilpailutus)))

(defn edit-kilpailutus! [kilpailutusid kilpailutus]
  (assert-modify-kilpailutus-allowed*! kilpailutus)
  (dml/update-where-id db "kilpailutus" kilpailutus kilpailutusid)
  nil)

(defn find-kilpailutukset [filter]
  (let [sql-body {:select (keys s/Kilpailutus)
                  :from [:kilpailutus]}]
    (map coerce-kilpailutus (jsql/query db (hsql/format sql-body) {}))))

(defn delete-kilpailutus! [kilpailutusid]
  (assert-modify-kilpailutus-allowed*! (get-kilpailutus! kilpailutusid))
  (delete-kilpailutus-where-id! (c/bindings->map kilpailutusid))
  nil)

(defn str->bigdec [^String txt]
  (try (BigDecimal. (str/replace (str/replace txt "," ".") #"\h" ""))
       (catch Throwable _ txt)))

(defn str->localdate [^String txt]
  (f/parse-local-date (f/formatter "dd.MM.yyyy") txt))

(defn str->nil [txt]
  (if (string? txt)
    (let [content (strx/trim txt)]
      (if (strx/not-blank? content) content nil))
    txt))

(defn import-kilpailutukset! [data]
  (let [header (map (comp keyword str/trim) (first data))
        default-values (m/map-values (constantly nil) s/EditKilpailutus)
        row->kilpailutus (fn [row] (into {} (map vector header (map str->nil row))))
        coercer (sc-coerce/coercer! s/EditKilpailutus (coerce/create-matcher {LocalDate #'str->localdate
                                                                              sc/Num #'str->bigdec}))]
    (doseq [row (rest data)]
      (add-kilpailutus! (coercer (merge default-values (row->kilpailutus row)))))))