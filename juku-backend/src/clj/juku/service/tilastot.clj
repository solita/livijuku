(ns juku.service.tilastot
  (:require [juku.db.yesql-patch :as sql]
            [juku.db.coerce :as coerce]
            [juku.db.sql :as jsql]
            [juku.schema.tunnusluku :as s]
            [juku.db.database :refer [db]]
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
  (c/error-let!
    [table (tunnusluku-table tunnusluku) nil?
      {:http-response r/bad-request :message (str "Tunnuslukua " tunnusluku " ei ole olemassa.")}
     fact-field (tunnusluku-field tunnusluku) nil?
      (str "Tunnusluvulle " tunnusluku " ei ole määritetty kenttää.")
     group-by-fields (vec (map group-by-field group-by)) (partial some nil?)
      {:http-response r/bad-request :message (str "Tunnusluku " tunnusluku " ei ei tue ryhmittelyä: " group-by)}]

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
                    :order-by sql-group-by}

          sql (if (empty? sql-where) sql-body (assoc sql-body :where (cons :and sql-where)))]

      (jsql/query db (hsql/format sql) {:as-arrays? true}))))

;; *** Avustustiedot avustuskuvaajia varten ***

(def avustus-data-2010-2015 {
  "KS1" {
        ;; Oulu, HSL, Tampere, Turku
        :organisaatiot
         [10, 1, 12, 13],
        :haetut
         (partition 6
           [320500  790000  988990  2653000 3649759 5600000
            5213000 6230000 5553495 8000000 8015000 5540000
            998530  1990872 2265000 3355000 3430000 3358000
            831311  989371  1457016 3357290 4400000 6875000])
        :myonnetyt
         (partition 6
           [320500  790000  913990  1314775 1751255 1339000
            5213309 6229319 5553495 5821548 6096802 4700000
            998530  1990872 1751951 2538098 2805226 2129000
            831311  989371  1457016 2377717 2069717 1582000])}

  "KS2" {
        ;; Hämeenlinna, Joensuu, Jyväskylä, Kotka, Kouvola, Kuopio, Lahti, Lappeenranta, Pori, Vaasa
        :organisaatiot
         [2, 3, 4, 5, 6, 7, 8, 9, 11, 14],
        :haetut
         (partition 6
           [1359492 1477181 1592264 1693099 1459000 1621130
            363542  437110  540702  539238  741300  972500
            1990000 1965000 1951500 2041050 5059250 1399000
            1180000 1111000 1174000 977000  1278000 1358000
            646800  966500  1123441 982000  1069000 1064600
            823313  1135340 1059000 1249100 1249100 1336500
            2187624 2389200 2110350 2679170 6775500 3873300
            700000  754330  838045  662160  1108829 1211000
            1180419 1323489 1375860 1622061 1439534 1483436
            732526  839377  914610  1031826 1281000 1250717])
        :myonnetyt
         (partition 6
           [838383  951391 1073901 985000 985000 946000
            338622  377650 457208 515000 515000 505000
            1294000 1266067 1115100 1238000 1238000 1213000
            864739  703223 702635 775000 775000 728000
            560004  625653 500230 555000 555000 544000
            722005  834432 1152741 1249100 1249100 1174000
            1009696 1074844 1228289 1380000 1380000 1352000
            479562  567670 558468 662160 662160 636000
            959244  960700 997200 1162000 1162000 1092000
            490000  482000 350000 395000 395000 390000])}

  "ELY" {
        ;; Pohjois-Pohjanmaa, Pohjois-Savo, Varsinais-Suomi, Uudenmaa, Etelä-Pohjanmaa, Kaakkois-Suomi, Keski-Suomi, Lappi, Pirkanmaa
        :organisaatiot
         [23, 20, 17, 16, 22, 18, 21, 24, 19],
        :haetut
         (partition 7
           [4726300 6190000  5740000 6026000  0 5083361 4272000
            8946000 10225000 9525222 7826094  0 6775000 6750000
            5044972 5885000  6900000 6985000  0 2980000 2620000
            5949922 8301474  11100000 6461517 0 9590918 11720374
            3589635 4308816  4340000 4113803  0 3664548 4208284
            3940058 4419818  5440000 1450669  0 1705803 1331000
            3425700 4405000  3960000 2535000  0 1764000 1903150
            3416000 3980000  3990000 4165000  0 3987000 3737782
            2988258 4392405  4120000 3762000  0 2415000 2744314])
        :myonnetyt
         (partition 7
            [4577000 4423000 4209000 4100000 3879000 3986000 3974000
             8653000 8594000 8250000 6900000 6695000 6420000 6325000
             4756000 4802000 4803000 2800000 2586000 2557000 2702000
             6272000 6225000 6303000 4600000 5095000 5518000 5617000
             3264000 3579000 3507000 3450000 3192000 3156000 3551000
             3539000 3662000 3457000 1450000 3065000 4150000 2781000
             3186000 3183000 3003000 1900000 1684000 1590000 1760000
             3284000 3293000 3324000 3350000 3138000 3053000 3086000
             3073000 3036000 3141000 2200000 1985000 1963000 2033000])}})

(def vuodet (range 2010 2017))

(def avustus-data+all
  (assoc avustus-data-2010-2015 "ALL" (apply m/deep-merge-with #(apply concat %) (vals avustus-data-2010-2015))))

(def sum (partial reduce +))

(defn data->avustus-tilasto [data]
  (concat
    (map vector (repeat "H") vuodet (apply mapv (comp sum vector) (:haetut data)))
    (map vector (repeat "M") vuodet (apply mapv (comp sum vector) (:myonnetyt data)))))

(def avustus-tilasto-2010-2015
  (m/map-values data->avustus-tilasto avustus-data+all))

(def avustus-tilasto-ely-2016
  {:haetut (nth (coll/find-first #(and (= (nth % 0) "H") (= (nth % 1) 2016)) (get avustus-tilasto-2010-2015 "ELY")) 2)
   :myonnetyt (nth (coll/find-first #(and (= (nth % 0) "M") (= (nth % 1) 2016)) (get avustus-tilasto-2010-2015 "ELY")) 2)})

(defn data->avustus-tilasto-organisaatio [data]
  (apply concat
         (map-indexed #(map vector (repeat %2) vuodet
                            (nth (:haetut data) %1)
                            (nth (:myonnetyt data) %1))
                        (:organisaatiot data))))

(def avustus-tilasto-organisaatio-2010-2015
  (m/map-values data->avustus-tilasto-organisaatio avustus-data+all))

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