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
            [clojure-csv.core :as csv]
            [slingshot.slingshot :as ss]
            [clojure.java.io :as io]
            [juku.service.common :as common])
  (:import (org.joda.time LocalDate)
           (java.io Writer)))

; *** Kilpailutuksiin liittyvät kyselyt ***
(sql/defqueries "kilpailutus.sql")

; *** Kilpailutus-skeemaan liittyvät konversiot tietokannan tietotyypeistä ***
(def coerce-kilpailutus (coerce/coercer s/Kilpailutus))
(def coerce-luokka (coerce/coercer js/Luokka))

(defn create-kilpailutus-coercer+authorization []
  (if (user/has-privilege* :view-kilpailutus-tarjoustieto)
    coerce-kilpailutus
    (fn [row] (assoc (coerce-kilpailutus row)
                :tarjousmaara  nil
                :tarjoushinta1 nil
                :tarjoushinta2 nil))))

; *** Virheviestit tietokannan rajoitteista ***
(def constraint-errors {
   :kilpailutus_organisaatio_fk {:http-response r/not-found :message "Kilpailutuksen organisaatiota {organisaatioid} ei ole olemassa."}})

(derive ::kilpailutus-not-found ::coll/not-found)

(defn find-sopimusmallit [] (map coerce-luokka (select-sopimusmallit)))

(defn find-kilpailutus [kilpailutusid]
  (first (map (create-kilpailutus-coercer+authorization) (select-kilpailutus (c/bindings->map kilpailutusid)))))

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

  [kilpailutus errormsg]
  (when-not (or (user/has-privilege* :modify-kaikki-kilpailutukset)
                (and (user/has-privilege* :modify-omat-kilpailutukset)
                     (== (:organisaatioid user/*current-user*) (:organisaatioid kilpailutus))))
    (forbidden! errormsg)))

(defn add-kilpailutus! [kilpailutus]
  (assert-modify-kilpailutus-allowed*! kilpailutus
    (str "Käyttäjällä " (:tunnus user/*current-user*) " ei ole oikeutta lisätä kilpailutuksia."))
  (:id (dml/insert-with-id db "kilpailutus" kilpailutus constraint-errors kilpailutus)))

(defn edit-kilpailutus! [kilpailutusid kilpailutus]
  (assert-modify-kilpailutus-allowed*! kilpailutus
    (str "Käyttäjällä " (:tunnus user/*current-user*) " ei ole oikeutta muokata kilpailutusta: " kilpailutusid "."))
  (dml/update-where-id db "kilpailutus" kilpailutus kilpailutusid constraint-errors kilpailutus)
  nil)

(defn find-kilpailutukset [filter]
  (let [sql-body {:select (keys s/Kilpailutus)
                  :from [:kilpailutus]}]
    (map (create-kilpailutus-coercer+authorization) (jsql/query db (hsql/format sql-body) {}))))

(defn delete-kilpailutus! [kilpailutusid]
  (assert-modify-kilpailutus-allowed*! (get-kilpailutus! kilpailutusid)
    (str "Käyttäjällä " (:tunnus user/*current-user*) " ei ole oikeutta poistaa kilpailutusta: " kilpailutusid "."))
  (delete-kilpailutus-where-id! (c/bindings->map kilpailutusid))
  nil)

(defn str->bigdec [^String txt]
  (try (BigDecimal. (str/replace (str/replace txt "," ".") #"\h" ""))
       (catch Throwable _ txt)))

(def default-localdate-formatter (f/formatter "dd.MM.yyyy"))

(defn str->localdate [^String txt]
  (f/parse-local-date default-localdate-formatter txt))

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

(defn date->str [date]
  (f/unparse-local-date default-localdate-formatter (coerce/date->localdate date)))

(def kilpailutus-formatters
  (m/map-values #(partial c/maybe-nil % "")
                {:selite                    coerce/clob->string
                 :julkaisupvm               date->str
                 :tarjouspaattymispvm       date->str
                 :hankintapaatospvm         date->str
                 :liikennointialoituspvm    date->str
                 :liikennointipaattymispvm  date->str
                 :hankittuoptiopaattymispvm date->str
                 :optiopaattymispvm         date->str
                 :kohdearvo                 common/format-number
                 :tarjoushinta1             common/format-number
                 :tarjoushinta2             common/format-number}))

(defn resultset->out-as-csv [output resultset]
  (let [header (first resultset)
        formatter (map #(or (kilpailutus-formatters %) str) header)
        ^Writer w (io/writer output)]

    ; UTF-8 Byte-order marker will clue Excel 2007+ in to the fact that you're using UTF-8
    ; see http://stackoverflow.com/questions/6002256/is-it-possible-to-force-excel-recognize-utf-8-csv-files-automatically
    (.write w "\uFEFF")

    (.write w (csv/write-csv [(map name header)] :delimiter ";"))
    (.flush w)
    (doseq [row (rest resultset)]
      (.write w (csv/write-csv [(map #(%1 %2) formatter row)] :delimiter ";"))
      (.flush w))))

(defn export-kilpailutukset-csv [output]
  (select-all-kilpailutukset {} {:connection db :as-arrays? true
                                 :result-set-fn (partial resultset->out-as-csv output)}))