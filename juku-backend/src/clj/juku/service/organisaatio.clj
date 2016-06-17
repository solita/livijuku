(ns juku.service.organisaatio
  (:require [juku.db.yesql-patch :as sql]
            [juku.db.database :refer [db]]
            [juku.db.coerce :as coerce]
            [juku.db.sql :as dml]
            [common.collection :as coll]
            [common.core :as c]
            [schema.coerce :as scoerce]))

(sql/defqueries "organisaatio.sql")

(defn organisaatiot [] (select-organisaatiot))

(defn hakija-organisaatiot [] (filter (coll/predicate not= :lajitunnus "LV") (select-organisaatiot)))

(defn find-organisaatio
  ([id] (find-organisaatio id (select-organisaatiot)))
  ([id organisaatiot] (coll/find-first (coll/eq :id id) organisaatiot)))


(derive ::organisaatio-not-found ::c/not-found)

(defn get-organisaatio! [organisaatioid]
  (c/assert-not-nil! (find-organisaatio organisaatioid)
                   {:type ::organisaatio-not-found
                    :organisaatioid organisaatioid
                    :message (str "Organisaatiota " organisaatioid " ei ole olemassa.")}))

(defn find-organisaatio-of [user] (find-organisaatio (:organisaatioid user)))

(defn find-unique-organisaatio-ext-tunnus-like [tunnus]
  (coll/single-result! (select-organisaatio-like-exttunnus {:tunnus tunnus})
                       {:message (str "Tunnisteella " tunnus " löytyy useita organisaatioita.")}))