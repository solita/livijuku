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
            [juku.service.hakemus-core :as hc]
            [common.map :as map]))

; *** Hakemukseen liittyvät kyselyt ***
(sql/defqueries "avustuskohde.sql")

; *** Hakemus skeemaan liittyvät konversiot tietokannan tietotyypeistä ***

(def coerce-avustuskohde (scoerce/coercer Avustuskohde+alv coerce/db-coercion-matcher))

; *** Virheviestit tietokannan rajoitteista ***
(def constraint-errors
  {:avustuskohde_pk {:http-response r/bad-request :message "Avustuskohde {avustuskohdeluokkatunnus}-{avustuskohdelajitunnus} on jo olemassa hakemuksella (id = {hakemusid})."}
   :avustuskohde_hakemus_fk {:http-response r/not-found :message "Avustuskohteen {avustuskohdeluokkatunnus}-{avustuskohdelajitunnus} hakemusta (id = {hakemusid}) ei ole olemassa."}
   :avustuskohde_aklaji_fk {:http-response r/not-found :message "Avustuskohdelajia {avustuskohdeluokkatunnus}-{avustuskohdelajitunnus} ei ole olemassa."}})

; *** Avustuskohteiden alv käsittely ***

(defn alv%
  "Avustuskohteen arvonlisäveroprosentti."
  [avustuskohde]
  (case (:avustuskohdeluokkatunnus avustuskohde)
    "K" 24
    10))

(defn include-alv?
  "Haetaanko ja maksetaanko avustuskohteen avustus arvonlisäverollisena."
  [avustuskohde]
  (case (:avustuskohdeluokkatunnus avustuskohde)
    "HK" true
    false))

(defn round
  "Rahamäärän pyöristäminen kahteen desimaaliin."
  [^BigDecimal moneyamount] (.setScale moneyamount 2 BigDecimal/ROUND_HALF_UP))

(defn +alv
  "Arvonlisäveron lisääminen arvonlisäverottomaan hintaan."
  [hinta-alv0 alv] (* hinta-alv0 (inc (/ alv 100))))

(defn avustus+alv
  "Avustuskohteen rahamäärien päivitys arvonlisäverollisiksi, jos ko. kohteen avustus pitää hakea arvonlisäverollisena.
  Muunnos sisältää myös tarvittavat pyöristykset"
  [avustus-alv0]
  (if (include-alv? avustus-alv0)
    (let [+alv #(round (bigdec (+alv % (alv% avustus-alv0))))]
      (map/update-vals avustus-alv0 [:haettavaavustus :omarahoitus] +alv))
    (map/update-vals avustus-alv0 [:haettavaavustus :omarahoitus] round)))

; *** Hakemuksen avustuskohteisiin liittyvät palvelut ***

(defn find-avustuskohteet-by-hakemusid [hakemusid]
  (map coerce-avustuskohde (map (fn [ak] (assoc ak :alv (alv% ak)
                                                   :includealv (include-alv? ak)))
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
        avustuskohteet+nimi (coll/assoc-join avustuskohteet :avustuskohdenimi avustuskohdelajit
                                             [:avustuskohdeluokkatunnus :avustuskohdelajitunnus]
                                             (comp :nimi first coll/children))]

    (str/join "\n" (map (partial xstr/interpolate avustuskohde-template)
                        (map (c/partial-first-arg update :haettavaavustus pdf/format-number) avustuskohteet+nimi)))))

(defn alv-title [kohde]
  (if (include-alv? kohde)
    (str "(alv " (alv% kohde) "%)")
    "(alv 0%)"))

(defn avustuskohdeluokka-nimi [kohde avustuskohdeluokat] (:nimi (get avustuskohdeluokat (:avustuskohdeluokkatunnus kohde))))

(defn avustuskohteet-section
  "Asiakirjojen avustuskohdelistauksen teksti. Avustuskohteen rahasummat pitää antaa alvillisina."
  [avustuskohteet]
  (let [avustuskohteet (filter (coll/predicate > :haettavaavustus 0) avustuskohteet)
        avustuskohdelajit (map #(set/rename-keys % {:tunnus :avustuskohdelajitunnus}) (select-avustuskohdelajit) )
        avustuskohdeluokat (m/map-values first (group-by :tunnus (select-avustuskohdeluokat)))
        avustuskohteet-luokittain (partition-by :avustuskohdeluokkatunnus avustuskohteet)
        avustuskohdeluokka-otsikko (fn [kohde] (str (avustuskohdeluokka-nimi kohde avustuskohdeluokat) " "
                                                    (alv-title kohde)))
        avustuskohdeluokka (fn [kohteet] (str "\t*" (avustuskohdeluokka-otsikko (first kohteet)) "\n"
                                              (avustuskohderivit kohteet avustuskohdelajit)))]

    (str/join "\n\n" (map avustuskohdeluokka avustuskohteet-luokittain))))

(defn total-haettavaavustus [avustuskohteet] (reduce + 0 (map :haettavaavustus avustuskohteet)))

(defn total-omarahoitus [avustuskohteet] (reduce + 0 (map :omarahoitus avustuskohteet)))

(defn avustuskohteet-summary
  "Avustuskohteiden yhteenveto erittely. Avustuskohteet summattu avustuskohdeluokittain.
  Avustuskohteen rahasummat annetaan alvittomina eli samassa muodossa kuin ne on tallennettu."
  [avustuskohteet]
  (let [avustuskohteet (filter (coll/predicate > :haettavaavustus 0) avustuskohteet)
        avustuskohdeluokat (m/map-values first (group-by :tunnus (select-avustuskohdeluokat)))
        avustuskohteet-luokittain (partition-by :avustuskohdeluokkatunnus avustuskohteet)
        avustuskohdeluokka (fn [kohteet]
                             (let [kohde (first kohteet)
                                   avustus (reduce + (map :haettavaavustus kohteet))
                                   alv (alv% kohde)]

                               (str "\t" (avustuskohdeluokka-nimi kohde avustuskohdeluokat) " "
                                    (pdf/format-number (if (include-alv? kohde) (+alv avustus alv) avustus)) " e "
                                    (alv-title kohde)
                                    (if (include-alv? kohde)
                                      (str " sisältäen arvonlisäveron osuuden "
                                           (pdf/format-number (* avustus (/ alv 100))) " e.")))))]

    (str/join "\n\n" (map avustuskohdeluokka avustuskohteet-luokittain))))

(defn avustuskohde-template-values [avustuskohteet]
  (let [avustuskohteet+alv (map avustus+alv avustuskohteet)]
    {:avustuskohteet (avustuskohteet-section avustuskohteet+alv)
     :haettuavustus (pdf/format-number (total-haettavaavustus avustuskohteet+alv))
     :omarahoitus (pdf/format-number (total-omarahoitus (filter (coll/predicate > :haettavaavustus 0)
                                                                avustuskohteet+alv)))
     :omarahoitus-all (pdf/format-number (total-omarahoitus avustuskohteet+alv))
     :avustuskohteet-summary (avustuskohteet-summary avustuskohteet)}))

(defn avustuskohde-template-values-by-hakemusid [hakemusid]
  (avustuskohde-template-values (find-avustuskohteet-by-hakemusid hakemusid)))

(defn find-avustuskohteet [hakemusid]
  (h/assert-view-hakemus-content-allowed*! (h/get-hakemus hakemusid))
  (find-avustuskohteet-by-hakemusid hakemusid))

