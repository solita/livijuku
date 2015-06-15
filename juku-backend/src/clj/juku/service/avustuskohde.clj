(ns juku.service.avustuskohde
  (:require [juku.db.yesql-patch :as sql]
            [juku.db.database :refer [db with-transaction]]
            [juku.db.coerce :as coerce]
            [juku.db.sql :as dml]
            [common.string :as xstr]
            [clojure.string :as str]
            [schema.coerce :as scoerce]
            [juku.schema.hakemus :refer :all]
            [ring.util.http-response :as r]
            [common.collection :as c]
            [clojure.set :as set]))

; *** Hakemukseen liittyvät kyselyt ***
(sql/defqueries "avustuskohde.sql")

; *** Hakemus skeemaan liittyvät konversiot tietokannan tietotyypeistä ***

(def coerce-avustuskohde (scoerce/coercer Avustuskohde+alv coerce/db-coercion-matcher))

; *** Virheviestit tietokannan rajoitteista ***
(def constraint-errors
  {:avustuskohde_pk {:http-response r/bad-request :message "Avustuskohde {avustuskohdeluokkatunnus}-{avustuskohdelajitunnus} on jo olemassa hakemuksella (id = {hakemusid})."}
   :avustuskohde_hakemus_fk {:http-response r/not-found :message "Avustuskohteen {avustuskohdeluokkatunnus}-{avustuskohdelajitunnus} hakemusta (id = {hakemusid}) ei ole olemassa."}
   :avustuskohde_aklaji_fk {:http-response r/not-found :message "Avustuskohdelajia {avustuskohdeluokkatunnus}-{avustuskohdelajitunnus} ei ole olemassa."}})

; *** Hakemukseen ja sen sisältöön liittyvät palvelut ***

(defn alv [avustuskohde]
  (if (= (:avustuskohdeluokkatunnus avustuskohde) "K") 24 10))

(defn find-avustuskohteet-by-hakemusid [hakemusid]
  (map coerce-avustuskohde (map (fn [ak] (assoc ak :alv (alv ak)))
                                (select-avustuskohteet {:hakemusid hakemusid}))))

(defn add-avustuskohde! [avustuskohde]
  (:id (dml/insert db "avustuskohde"
                           (-> avustuskohde
                               (dissoc :alv) ;; TODO remove
                               coerce/object->row
                               coerce/localdate->sql-date)
                           constraint-errors avustuskohde)))

(defn save-avustuskohde! [avustuskohde]
  (if (= (update-avustuskohde! avustuskohde) 0)
    (add-avustuskohde! avustuskohde)))

(defn save-avustuskohteet! [avustuskohteet]
  (with-transaction
    (doseq [avustuskohde avustuskohteet]
      (save-avustuskohde! avustuskohde))))

(defn avustuskohde-luokittelu []
  (let [luokat (select-avustuskohdeluokat)
        lajit (select-avustuskohdelajit)
        lajit-group-by-luokka (group-by :avustuskohdeluokkatunnus lajit)
        assoc-avustuskohdelajit (fn [luokka] (assoc luokka :avustuskohdelajit (get lajit-group-by-luokka (:tunnus luokka))))]
    (map assoc-avustuskohdelajit luokat)))

(defn avustuskohteet-section [avustuskohteet]
  (let [avustuskohde-template "\t{avustuskohdenimi}\t\t{haettavaavustus} e"
        avustuskohdelajit (map #(set/rename-keys % {:tunnus :avustuskohdelajitunnus}) (select-avustuskohdelajit) )
        avustuskohteet (filter (c/predicate > :haettavaavustus 0) avustuskohteet)
        avustuskohteet+nimi (c/join avustuskohteet
                                    (fn [akohde, aklajiseq] (assoc akohde :avustuskohdenimi (:nimi (first aklajiseq))))
                                    avustuskohdelajit [:avustuskohdeluokkatunnus :avustuskohdelajitunnus])]

    (str/join "\n" (map (partial xstr/interpolate avustuskohde-template) avustuskohteet+nimi))))

(defn total-haettavaavustus [avustuskohteet] (reduce + 0 (map :haettavaavustus avustuskohteet)))

(defn total-omarahoitus [avustuskohteet] (reduce + 0 (map :omarahoitus avustuskohteet)))

