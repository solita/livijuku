(ns juku.service.tilastot
  (:require [juku.db.yesql-patch :as sql]
            [juku.db.coerce :as coerce]
            [juku.db.sql :as jsql]
            [juku.schema.tunnusluku :as s]
            [juku.db.database :refer [db]]
            [juku.service.avustushistoria :as ah]
            [juku.service.user :as u]
            [ring.util.http-response :as r]
            [juku.db.sql :as dml]
            [slingshot.slingshot :as ss]
            [common.core :as c]
            [clojure.string :as str]
            [yesql.generate :as generate]
            [clojure.java.jdbc :as jdbc]
            [honeysql.core :as hsql]
            [honeysql.helpers :as hsql-h]
            [juku.service.organisaatio :as o]
            [common.collection :as coll]
            [common.map :as m]))


; *** Tilastoihin liittyvät kyselyt ***
(sql/defqueries "tilastot.sql")

(def demo-data {
  :kuukausi (map str/join (c/cartesian-product [(range 2013 2016) (map (partial format "%02d") (range 1 13))]))
  :paastoluokkatunnus (map (partial str "E") (range 0,7))
  :viikonpaivaluokkatunnus ["A", "LA", "SU"]
  :kustannuslajitunnus ["AP", "KP", "LP", "TM", "MP"]
  :vyohykemaara (range 1 7)
  :lippuhintaluokkatunnus ["KE" "KA"]
  :vuosi (range 2013 2016)})

(defn tunnusluku-tilasto-demo [tunnusluku organisaatiolajitunnus where group-by]
  (let [column1 (if (= organisaatiolajitunnus "ALL")
                  (map :id (o/organisaatiot))
                  (map :id (filter (coll/eq :lajitunnus organisaatiolajitunnus) (o/organisaatiot))))
        next-columns (map (comp demo-data keyword) (rest group-by))]
    (map #(concat % [(rand-int 100)]) (c/cartesian-product (cons column1 next-columns)))))


(def join-organisaatio (hsql-h/join :organisaatio [:= :t.organisaatioid :organisaatio.id]))

(def tunnusluku-field
  {:nousut                 :nousut
   :lahdot                 :lahdot
   :linjakilometrit        :linjakilometrit
   :nousut-viikko          :nousut
   :lahdot-viikko          :lahdot
   :linjakilometrit-viikko :linjakilometrit
   :liikennointikorvaus    :korvaus
   :lipputulo              :tulo
   :kalusto                :lukumaara
   :kustannukset           :kustannus
   :lippuhinnat            :hinta

   :alue-kuntamaara   :kuntamaara
   :alue-vyohykemaara :vyohykemaara
   :alue-pysakkimaara :pysakkimaara
   :alue-maapintaala  :maapintaala
   :alue-asukasmaara  :asukasmaara
   :alue-tyopaikkamaara :tyopaikkamaara
   :alue-henkilosto     :henkilosto
   :alue-pendeloivienosuus          :pendeloivienosuus
   :alue-henkiloautoliikennesuorite :henkiloautoliikennesuorite
   :alue-autoistumisaste            :autoistumisaste
   :alue-asiakastyytyvaisyys        :asiakastyytyvaisyys })

(def tunnusluku-table
  {:nousut                 :fact_liikenne
   :lahdot                 :fact_liikenne
   :linjakilometrit        :fact_liikenne
   :nousut-viikko          :fact_liikenneviikko
   :lahdot-viikko          :fact_liikenneviikko
   :linjakilometrit-viikko :fact_liikenneviikko
   :liikennointikorvaus    :fact_liikennointikorvaus
   :lipputulo              :fact_lipputulo_unpivot_view
   :kalusto                :fact_kalusto
   :kustannukset           :fact_kustannus_unpivot_view
   :lippuhinnat            :fact_lippuhinta_unpivot_view

   :alue-kuntamaara        :fact-alue
   :alue-vyohykemaara      :fact-alue
   :alue-pysakkimaara      :fact-alue
   :alue-maapintaala       :fact-alue
   :alue-asukasmaara       :fact-alue
   :alue-tyopaikkamaara    :fact-alue
   :alue-henkilosto        :fact-alue
   :alue-pendeloivienosuus :fact-alue
   :alue-henkiloautoliikennesuorite :fact-alue
   :alue-autoistumisaste     :fact-alue
   :alue-asiakastyytyvaisyys :fact-alue})

(def group-by-field
  {:organisaatioid :organisaatioid
   :vuosi :vuosi
   :kuukausi [(hsql/raw "vuosi || lpad(kuukausi, 2, '0')") :vuosikk]
   :viikonpaivaluokkatunnus :viikonpaivaluokkatunnus
   :paastoluokkatunnus :paastoluokkatunnus
   :kustannuslajitunnus :kustannuslajitunnus
   :vyohykemaara :vyohykemaara
   :lippuhintaluokkatunnus :lippuhintaluokkatunnus
   })

(defn tunnusluku-tilasto [tunnusluku organisaatiolajitunnus where group-by]
  (when (= tunnusluku :kustannukset)
    (u/assert-has-privilege*!
      :view-tunnusluku-kustannus
      (str "Käyttäjällä " (:tunnus u/*current-user*) " ei ole oikeutta nähdä kustannustietoja.")))

  (c/error-let!
    [table (tunnusluku-table tunnusluku) nil?
      {:http-response r/bad-request :message (str "Tunnuslukua " tunnusluku " ei ole olemassa.")}
     fact-field (tunnusluku-field tunnusluku) nil?
      (str "Tunnusluvulle " tunnusluku " ei ole määritetty kenttää.")
     group-by-fields (vec (map group-by-field group-by)) (partial some nil?)
      {:http-response r/bad-request :message (str "Tunnusluku " tunnusluku " ei tue ryhmittelyä: " group-by)}]

    (let [tunnusluku-where (map (fn [e] [:= (keyword (str "t." (name (first e)))) (second e)])
                                (filter (coll/predicate not= second nil) where))
          sql-where (if (= organisaatiolajitunnus "ALL")
                        tunnusluku-where
                        (conj tunnusluku-where [:= :organisaatio.lajitunnus organisaatiolajitunnus]))

          sql-group-by (map #(if (coll? %) (first %) %) group-by-fields)

          sql-body {:select (conj group-by-fields [(hsql/call :sum fact-field) :tunnusluku])
                    :from [[table :t]]
                    :join [:organisaatio [:= :t.organisaatioid :organisaatio.id]]
                    :group-by sql-group-by
                    :order-by (reverse sql-group-by)}

          sql (if (empty? sql-where) sql-body (assoc sql-body :where (cons :and sql-where)))]

      (jsql/query db (hsql/format sql) {:as-arrays? true}))))

;; *** Avustustiedot avustuskuvaajia varten ***

(def sum (partial reduce +))

(defn data->avustus-tilasto [data]
  (concat
    (map vector (repeat "H") ah/vuodet (apply mapv (comp sum vector) (:haetut data)))
    (map vector (repeat "M") ah/vuodet (apply mapv (comp sum vector) (:myonnetyt data)))))

(def avustus-tilasto-2010-2015
  (m/map-values data->avustus-tilasto ah/avustus-data+all))

(def avustus-tilasto-ely-2016
  {:haetut (nth (coll/find-first #(and (= (nth % 0) "H") (= (nth % 1) 2016)) (get avustus-tilasto-2010-2015 "ELY")) 2)
   :myonnetyt (nth (coll/find-first #(and (= (nth % 0) "M") (= (nth % 1) 2016)) (get avustus-tilasto-2010-2015 "ELY")) 2)})

(defn data->avustus-tilasto-organisaatio [data]
  (apply concat
         (map-indexed #(map vector (repeat %2) ah/vuodet
                            (nth (:haetut data) %1)
                            (nth (:myonnetyt data) %1))
                        (:organisaatiot data))))

(def avustus-tilasto-organisaatio-2010-2015
  (m/map-values data->avustus-tilasto-organisaatio ah/avustus-data+all))

(defn include-old-data [old-data new-data]
  (cons (first new-data) ;; header
    (concat old-data (rest new-data))))

(defn if-all-add-ely-2016 [organisaatiolajitunnus data]
  (if (= organisaatiolajitunnus "ALL")
    (map (fn [row]
           (if (= (nth row 1) 2016M)
             (case (nth row 0)
               "H" (update row 2 (partial (c/nil-safe +) (:haetut avustus-tilasto-ely-2016)))
               "M" (update row 2 (partial (c/nil-safe +) (:myonnetyt avustus-tilasto-ely-2016))))
             row)) data)
    data))

(defn avustus-tilasto [organisaatiolajitunnus]
  (if-all-add-ely-2016 organisaatiolajitunnus
    (include-old-data
      (get avustus-tilasto-2010-2015 organisaatiolajitunnus)
      (if (= organisaatiolajitunnus "KS3")
        (select-avustus-ks3-group-by-vuosi {} {:as-arrays? true :connection db})
        (select-avustus-group-by-vuosi (c/bindings->map organisaatiolajitunnus)
                                       {:as-arrays? true :connection db})))))

(defn avustus-organisaatio-tilasto [organisaatiolajitunnus]
  (include-old-data
    (get avustus-tilasto-organisaatio-2010-2015 organisaatiolajitunnus)
    (if (= organisaatiolajitunnus "KS3")
      (select-avustus-ks3-group-by-organisaatio-vuosi {} {:as-arrays? true :connection db})
      (select-avustus-group-by-organisaatio-vuosi (c/bindings->map organisaatiolajitunnus)
                                                  {:as-arrays? true :connection db}))))

(defn avustus-asukastakohti-tilasto-2010-2015 [organisaatiolajitunnus]
  (let [asukasmaarat (or (select-asukasmaara-2010-2015 (c/bindings->map organisaatiolajitunnus)) [])
        avustustilasto (or (get avustus-tilasto-organisaatio-2010-2015 organisaatiolajitunnus) [])
        nth* (fn [idx] (fn [coll] (-> coll (nth idx) bigdec)))]
    (coll/join asukasmaarat
               (fn [asukasmaara avustus]
                 [(:organisaatioid asukasmaara)
                  (:vuosi asukasmaara)
                  (with-precision
                    10 :rounding HALF_UP
                      ((c/nil-safe /) (nth (first avustus) 3) (:asukasmaara asukasmaara)))])
               avustustilasto {:organisaatioid (nth* 0) :vuosi (nth* 1)})))

(defn avustus-asukastakohti-tilasto [organisaatiolajitunnus]
  (include-old-data
    (avustus-asukastakohti-tilasto-2010-2015 organisaatiolajitunnus)
    (if (= organisaatiolajitunnus "KS3")
      (select-avustus-asukastakohti-ks3-group-by-organisaatio-vuosi {} {:as-arrays? true :connection db})
      (select-avustus-asukastakohti-group-by-organisaatio-vuosi (c/bindings->map organisaatiolajitunnus)
                                                                {:as-arrays? true :connection db}))))

(defn omarahoitus-asukastakohti-tilasto [organisaatiolajitunnus]
  (select-omarahoitus-asukastakohti-group-by-organisaatio-vuosi
    (c/bindings->map organisaatiolajitunnus) {:as-arrays? true :connection db}))

;; *** PSA-liikenteen nettokustannukset ***
(defn psa-nettokustannus [organisaatiolajitunnus]
  (select-psa-nettokustannus
    (c/bindings->map organisaatiolajitunnus) {:as-arrays? true :connection db}))
