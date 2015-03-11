(ns juku.service.test
  (:require [common.collection :as c]
            [juku.service.hakemuskausi :as k]
            [juku.db.database :refer [db]]
            [yesql.core :as sql]))

(sql/defqueries "juku/service/test.sql" {:connection db})

(defn find-next-notcreated-hakemuskausi []
  (int (+ (:next (first (select-max-vuosi-from-hakemuskausi))) 1)))

(defn init-hakemuskausi! [vuosi]
  (k/init-hakemuskausi! vuosi)
  (first (filter (c/eq :vuosi vuosi) (k/find-hakemuskaudet+summary))))

(defn next-hakemuskausi! []
  (let [vuosi (find-next-notcreated-hakemuskausi)] (init-hakemuskausi! vuosi)))

(defn hakemus-summary [hakemuskausi hakemustyyppi]
  (first (filter (c/eq :hakemustyyppitunnus hakemustyyppi) (:hakemukset hakemuskausi))))