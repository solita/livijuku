(ns juku.service.user
  (:require [juku.db.yesql-patch :as sql]
            [juku.db.sql :as dml]
            [juku.db.database :refer [db with-transaction]]
            [juku.db.coerce :as coerce]
            [schema.coerce :as scoerce]
            [juku.schema.user :as s]
            [clj-time.core :as time]
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

(def coerce-user (coerce/coercer s/User))
(def coerce-user+roles (coerce/coercer s/User+Roles))

(defn process-names [user]
  (m/dissoc-if-nil user :nimi :etunimi :sukunimi))

(defn find-privileges [roolit]
  (map (comp keyword :tunnus) (select-oikeudet-where-roolit-in {:roolit roolit})))

(defn find-user [tunnus]
  (first (map (comp coerce-user process-names) (select-user {:tunnus tunnus}))))

(def dbuser->user (comp coerce-user+roles process-names))

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

(defn has-privilege [privilege user]
  (c/not-nil? (some #{privilege} (:privileges user))))

(defn has-privilege* [privilege]
  (has-privilege privilege *current-user*))

(defn find-roleids [ssogroups]
  (map :tunnus (select-roolitunnukset {:ssogroup ssogroups})))

(defn find-user-rolenames [uid]
  (map :nimi (select-rolenames {:tunnus uid})))

(defn find-user-roles [uid]
  (map :kayttajaroolitunnus (select-roles {:tunnus uid})))

(defn update-roles! [uid roles]
  (let [old-roles (find-user-roles uid)
        sql-params {:tunnus uid :roles roles}]
    (if (not= (set roles) (set old-roles))
      (with-transaction
        (delete-previous-roles! sql-params)
        (insert-new-roles! sql-params)
        true)
      false)))

(defn update-kirjautumisaika [uid]
  (update-kirjautumisaika {:tunnus uid}))

(defn save-user! [user]
  (let [uid (:tunnus *current-user*)]
    (update-user! uid (dissoc user :tunnus))
    (assoc (merge *current-user* user) :roolit (find-user-rolenames uid))))

(defn delete-user! [tunnus]
  (update-kayttaja-mark-deleted! {:tunnus tunnus})
  nil)

(defn current-user+updatekirjautumisaika! []
  (let [uid (:tunnus *current-user*)]
    (assoc (save-user! {:tunnus uid :kirjautumisaika (time/now)})
          :roolit (find-user-rolenames uid))))
