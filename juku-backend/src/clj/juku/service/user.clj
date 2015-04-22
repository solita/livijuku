(ns juku.service.user
  (:require [juku.db.yesql-patch :as sql]
            [juku.db.sql :as dml]
            [juku.db.database :refer [db]]
            [juku.db.coerce :as coerce]
            [schema.coerce :as scoerce]
            [juku.schema.user :as s]
            [clojure.string :as str]
            [ring.util.http-response :as r]
            [common.map :as m]))

(sql/defqueries "user.sql")

; *** Virheviestit tietokannan rajoitteista ***
(def constraint-errors
  {:kayttaja_organisaatio_fk {:http-response r/not-found :message "Organisaatiota {organisaatioid} ei ole olemassa."}})

(def ^:dynamic *current-user*)

(defn user-fullname [user]
  (str (:etunimi user) " " (:sukunimi user)))

(defn with-user*
  [user f]
  (binding [*current-user* user] (f)))

(defmacro with-user [user & body]
  `(with-user* ~user (fn [] ~@body)))

(def coerce-user (scoerce/coercer s/User coerce/db-coercion-matcher))

(defn process-names [user]
  (m/dissoc-if-nil user :nimi :etunimi :sukunimi))

(defn find-user [tunnus]
  (first(map (comp coerce-user process-names) (select-user {:tunnus tunnus}))))

(defn find-privileges [roolit]
  (map :tunnus (select-oikeudet-where-roolit-in {:roolit roolit})))

(defn find-users-by-organization [organisaatioid]
  (map (comp coerce-user (fn [user] (m/dissoc-if-nil user :nimi :etunimi :sukunimi)))
       (select-users-where-organization {:organisaatioid organisaatioid})))

(defn create-user! [uid user]
  (dml/insert db "kayttaja" (assoc user :tunnus uid) constraint-errors user))

(defn update-user! [tunnus user]
  (dml/update-where! db "kayttaja" user {:tunnus tunnus}))