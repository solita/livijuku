(ns juku.service.user
  (:require [juku.db.yesql-patch :as sql]
            [juku.db.sql :as dml]
            [juku.db.database :refer [db with-transaction]]
            [juku.db.coerce :as coerce]
            [schema.coerce :as scoerce]
            [juku.schema.user :as s]
            [clojure.string :as str]
            [ring.util.http-response :as r]
            [common.map :as m]
            [common.core :as c]))

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
  (first (map (comp coerce-user process-names) (select-user {:tunnus tunnus}))))

(defn find-privileges [roolit]
  (map (comp keyword :tunnus) (select-oikeudet-where-roolit-in {:roolit roolit})))

(def dbuser->user (comp coerce-user (fn [user] (m/dissoc-if-nil user :nimi :etunimi :sukunimi))))

(defn find-users-by-organization [organisaatioid]
  (map dbuser->user (select-users-where-organization {:organisaatioid organisaatioid})))

(defn find-all-users []
  (map dbuser->user (select-all-human-users)))

(defn find-all-livi-users []
  (map dbuser->user (select-all-livi-users)))

(defn create-user! [uid user]
  (dml/insert db "kayttaja" (assoc user :tunnus uid) constraint-errors user))

(defn update-user! [tunnus user]
  (dml/update-where! db "kayttaja" user {:tunnus tunnus}))

(defn save-user! [user]
  (do (update-user! (:tunnus *current-user*) user)
      (merge *current-user* user)))

(defn has-privilege [privilege user]
  (c/not-nil? (some #{privilege} (:privileges user))))

(defn has-privilege* [privilege]
  (has-privilege privilege *current-user*))

(defn find-roolitunnukset [ssogroups]
  (map :tunnus (select-roolitunnukset {:ssogroup ssogroups})))

(defn update-roles! [uid ssogroups]
  (let [old-roles (map :kayttajaroolitunnus (select-roles {:tunnus uid}))
        roles (find-roolitunnukset ssogroups)
        sql-params {:tunnus uid :roles roles}]
    (if (not= (set roles) (set old-roles))
      (with-transaction
        (delete-previous-roles! sql-params)
        (insert-new-roles! sql-params)
        true)
      false)))