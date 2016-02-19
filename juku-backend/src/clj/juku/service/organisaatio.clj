(ns juku.service.organisaatio
  (:require [juku.db.yesql-patch :as sql]
            [clojure.java.jdbc :as jdbc]
            [juku.db.database :refer [db]]
            [juku.db.coerce :as coerce]
            [juku.db.sql :as dml]
            [common.collection :as c]
            [schema.coerce :as scoerce]))

(sql/defqueries "organisaatio.sql")

(defn organisaatiot [] (select-organisaatiot))

(defn hakija-organisaatiot [] (filter (c/predicate not= :lajitunnus "LV") (select-organisaatiot)))

(defn find-organisaatio
  ([id] (find-organisaatio (select-organisaatiot)))
  ([id organisaatiot] (c/find-first (c/eq :id id) organisaatiot)))

(defn find-organisaatio-of [user] (find-organisaatio (:organisaatioid user)))

(defn find-unique-organisaatio-ext-tunnus-like [tunnus]
  (c/single-result! (select-organisaatio-like-exttunnus {:tunnus tunnus})
                    {:message (str "Tunnisteella " tunnus " löytyy useita organisaatioita.")}))