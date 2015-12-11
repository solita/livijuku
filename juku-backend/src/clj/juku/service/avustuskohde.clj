(ns juku.service.avustuskohde
  (:require [juku.db.yesql-patch :as sql]
            [juku.db.database :refer [db with-transaction]]
            [juku.db.coerce :as coerce]
            [juku.db.sql :as dml]
            [juku.service.common :as service]
            [juku.service.user :as user]
            [juku.service.hakemus-core :as h]
            [common.string :as xstr]
            [common.map :as m]
            [juku.service.pdf :as pdf]
            [slingshot.slingshot :as ss]
            [clojure.string :as str]
            [schema.coerce :as scoerce]
            [juku.schema.hakemus :refer :all]
            [ring.util.http-response :as r]
            [common.collection :as coll]
            [clojure.set :as set]
            [common.core :as c]
            [juku.service.hakemus-core :as hc]))

; *** Hakemukseen liittyvät kyselyt ***
(sql/defqueries "avustuskohde.sql")

; *** Hakemus skeemaan liittyvät konversiot tietokannan tietotyypeistä ***

(def coerce-avustuskohde (scoerce/coercer Avustuskohde+alv coerce/db-coercion-matcher))

; *** Virheviestit tietokannan rajoitteista ***
(def constraint-errors
  {:avustuskohde_pk {:http-response r/bad-request :message "Avustuskohde {avustuskohdeluokkatunnus}-{avustuskohdelajitunnus} on jo olemassa hakemuksella (id = {hakemusid})."}
   :avustuskohde_hakemus_fk {:http-response r/not-found :message "Avustuskohteen {avustuskohdeluokkatunnus}-{avustuskohdelajitunnus} hakemusta (id = {hakemusid}) ei ole olemassa."}
   :avustuskohde_aklaji_fk {:http-response r/not-found :message "Avustuskohdelajia {avustuskohdeluokkatunnus}-{avustuskohdelajitunnus} ei ole olemassa."}})

; *** Hakemuksen avustuskohteisiin liittyvät palvelut ***

(defn alv
  "Avustuskohteen arvonlisäveroprosentti"
  [avustuskohde]
  (case (:avustuskohdeluokkatunnus avustuskohde)
    "K" 24
    10))

(defn include-alv
  "Haetaanko avustuskohteen avustus arvonlisäverollisena."
  [avustuskohde]
  (case (:avustuskohdeluokkatunnus avustuskohde)
    "HK" true
    false))

(defn find-avustuskohteet-by-hakemusid [hakemusid]
  (map coerce-avustuskohde (map (fn [ak] (assoc ak :alv (alv ak)
                                                   :include-alv (include-alv ak)))
                                (select-avustuskohteet {:hakemusid hakemusid}))))

(defn add-avustuskohde! [avustuskohde]
  (:id (dml/insert db "avustuskohde"
                           (-> avustuskohde
                               coerce/object->row)
                           constraint-errors avustuskohde)))

(defn save-avustuskohde! [avustuskohde]
  (if (= (update-avustuskohde! avustuskohde) 0)
    (add-avustuskohde! avustuskohde))) ;; TODO remove add-avustuskohde

(defn save-avustuskohteet! [avustuskohteet]
  (doseq [hakemus (select-hakemukset {:hakemusids (vec (set (map :hakemusid avustuskohteet)))})]
    (hc/assert-edit-hakemus-content-allowed*! hakemus))
  (with-transaction
    (doseq [avustuskohde avustuskohteet]
      (save-avustuskohde! avustuskohde))))

(defn avustuskohde-luokittelu []
  (let [luokat (select-avustuskohdeluokat)
        lajit (select-avustuskohdelajit)
        lajit-group-by-luokka (group-by :avustuskohdeluokkatunnus lajit)
        assoc-avustuskohdelajit (fn [luokka] (assoc luokka :avustuskohdelajit (get lajit-group-by-luokka (:tunnus luokka))))]
    (map assoc-avustuskohdelajit luokat)))

(defn avustuskohderivit [avustuskohteet avustuskohdelajit]
  (let [avustuskohde-template "\t{avustuskohdenimi}\t\t\t\t\t{haettavaavustus} e"
        avustuskohteet+nimi (coll/join (map (c/partial-first-arg update-in [:haettavaavustus] pdf/format-number) avustuskohteet)
                                       (fn [akohde, aklajiseq] (assoc akohde :avustuskohdenimi (:nimi (first aklajiseq))))
                                       avustuskohdelajit [:avustuskohdeluokkatunnus :avustuskohdelajitunnus])]

    (str/join "\n" (map (partial xstr/interpolate avustuskohde-template) avustuskohteet+nimi))))

(defn avustuskohteet-section [avustuskohteet]
  (let [avustuskohteet (filter (coll/predicate > :haettavaavustus 0) avustuskohteet)
        avustuskohdelajit (map #(set/rename-keys % {:tunnus :avustuskohdelajitunnus}) (select-avustuskohdelajit) )
        avustuskohdeluokat (m/map-values first (group-by :tunnus (select-avustuskohdeluokat)))
        avustuskohteet-luokittain (partition-by :avustuskohdeluokkatunnus avustuskohteet)
        avustuskohdeluokka-otsikko (fn [kohde] (:nimi (get avustuskohdeluokat (:avustuskohdeluokkatunnus kohde))))
        avustuskohdeluokka (fn [kohteet] (str "\t*" (avustuskohdeluokka-otsikko (first kohteet)) "\n"
                                              (avustuskohderivit kohteet avustuskohdelajit)))]

    (str/join "\n\n" (map avustuskohdeluokka avustuskohteet-luokittain))))

(defn total-haettavaavustus [avustuskohteet] (reduce + 0 (map :haettavaavustus avustuskohteet)))

(defn total-omarahoitus [avustuskohteet] (reduce + 0 (map :omarahoitus avustuskohteet)))

(defn avustuskohde-template-values [avustuskohteet]
  {:avustuskohteet (avustuskohteet-section avustuskohteet)
   :haettuavustus (pdf/format-number (total-haettavaavustus avustuskohteet))
   :omarahoitus (pdf/format-number (total-omarahoitus avustuskohteet))})

(defn avustuskohde-template-values-by-hakemusid [hakemusid]
  (avustuskohde-template-values (find-avustuskohteet-by-hakemusid hakemusid)))

(defn find-avustuskohteet [hakemusid]
  (h/assert-view-hakemus-content-allowed*! (h/get-hakemus hakemusid))
  (find-avustuskohteet-by-hakemusid hakemusid))

