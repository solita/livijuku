(ns juku.service.paatos
  (:require [yesql.core :as sql]
            [clojure.java.jdbc :as jdbc]
            [juku.db.database :refer [db]]
            [juku.db.coerce :as coerce]
            [juku.service.hakemus :as h]
            [schema.coerce :as scoerce]
            [juku.schema.paatos :as s]
            [common.collection :as c]))

(sql/defqueries "paatos.sql" {:connection db})

(def coerce-paatos (scoerce/coercer s/Paatos coerce/db-coercion-matcher))

(defn find-paatos [hakemusid]
  (first (map coerce-paatos (select-paatos {:hakemusid hakemusid}))))

(defn new-paatos! [paatos] (insert-paatos! paatos))

(defn- assert-update [updateamount hakemusid]
  (assert (< updateamount 2) (str "Tietokannan tiedot ovat virheelliset. Hakemuksella " hakemusid " on kaksi avointa päätöstä.")))

(defn save-paatos! [paatos]
  (let [updated (update-paatos! paatos)]
    (assert-update updated (:hakemusid paatos))
    (if (== updated 0) (new-paatos! paatos))
    nil))

(defn hyvaksy-paatos! [hakemusid]
  (jdbc/with-db-transaction [db-spec db]
     (let [updated (update-paatos-hyvaksytty! {:hakemusid hakemusid})]
        (assert-update updated hakemusid)
        (cond
           (== updated 1) (h/update-hakemustila! {:hakemusid hakemusid :hakemustilatunnus "P"})
           (== updated 0) (c/not-found! ::paatos-not-found {:hakemusid hakemusid} (str "Hakemuksella " hakemusid " ei ole avointa päätöstä")))))
  nil)
