(ns juku.service.kilpailutus
  (:require [juku.db.yesql-patch :as sql]
            [juku.db.database :refer [db with-transaction]]
            [juku.db.coerce :as coerce]
            [juku.schema.kilpailutus :as s]
            [ring.util.http-response :as r]
            [juku.db.sql :as dml]
            [common.core :as c]
            [honeysql.core :as hsql]
            [juku.db.sql :as jsql]))

; *** Kilpailutuksiin liittyvät kyselyt ***
(sql/defqueries "kilpailutus.sql")

; *** Kilpailutus-skeemaan liittyvät konversiot tietokannan tietotyypeistä ***
(def coerce-kilpailutus (coerce/coercer s/Kilpailutus))

; *** Virheviestit tietokannan rajoitteista ***
(def constraint-errors {
   :kilpailutus_organisaatio_fk {:http-response r/not-found :message "Kilpailutuksen organisaatiota {organisaatioid} ei ole olemassa."}})

(defn find-kilpailutus [kilpailutusid]
  (first (map coerce-kilpailutus (select-kilpailutus (c/bindings->map kilpailutusid)))))

(defn add-kilpailutus [kilpailutus]
  (:id (dml/insert-with-id db "kilpailutus" kilpailutus constraint-errors kilpailutus)))

(defn edit-kilpailutus [kilpailutusid kilpailutus]
  (dml/update-where-id db "kilpailutus" kilpailutus kilpailutusid)
  nil)

(defn find-kilpailutukset [filter]
  (let [sql-body {:select (keys s/Kilpailutus)
                  :from [:kilpailutus]}]
    (map coerce-kilpailutus (jsql/query db (hsql/format sql-body) {}))))