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
(sql/defqueries "tilastot.sql" {:as-arrays? true})

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
         :organisaatiot [10, 1, 12, 13],
         :asukasmaara   [150000, 1100000, 275000, 180000],
         :haetut {
                         2010 [320500, 5213000, 998530, 831311],
                         2011 [790000, 6230000, 1990872, 989371],
                         2012 [988990, 5553495, 2265000, 1457016],
                         2013 [2653000, 8000000, 3355000, 3357290],
                         2014 [3649759, 8015000, 3430000, 4400000]}
         :myonnetyt {
                         2010 [320500, 5213309, 998530, 831311],
                         2011 [790000, 6229319, 1990872, 989371],
                         2012 [913990, 5553495, 1751951, 1457016],
                         2013 [1314775, 5821548, 2538098, 2377717],
                         2014 [1751255, 6096802, 2805226, 2069717]}},

  "KS2" {
  ;; Hämeenlinna, Joensuu, Jyväskylä, Kotka, Kouvola, Kuopio, Lahti, Lappeenranta, Pori, Vaasa
         :organisaatiot [2, 3, 4, 5, 6, 7, 8, 9, 11, 14],
         :asukasmaara   [70000, 55000, 65000, 480000, 35000, 40000, 69000, 42000, 35000, 37000],
         :haetut {
                         2010 [1359492, 363542, 1990000, 1180000, 646800, 823313, 2187624, 700000, 1180419, 732526],
                         2011 [1477181, 437110, 1965000, 1111000, 966500, 1135340, 2389200, 754330, 1323489, 839377],
                         2012 [1592264, 540702, 1951500, 1174000, 1123441, 1059000, 2110350, 838045, 1375860, 914610],
                         2013 [1693099, 539238, 2041050, 977000, 982000, 1249100, 2679170, 662160, 1622061, 1031826],
                         2014 [1459000, 741300, 5059250, 1278000, 1069000, 1249100, 6775500, 1108829, 1439534, 1281000]}
         :myonnetyt {
                         2010 [838383, 338622, 1294000, 864739, 560004, 722005, 1009696, 479562, 959244, 490000],
                         2011 [951391, 377650, 1266067, 703223, 625653, 834432, 1074844, 567670, 960700, 482000],
                         2012 [1073901, 457208, 1115100, 702635, 500230, 1152741, 1228289, 558468, 997200, 350000],
                         2013 [985000, 515000, 1238000, 775000, 555000, 1249100, 1380000, 662160, 1162000, 395000],
                         2014 [985000, 515000, 1238000, 775000, 555000, 1249100, 1380000, 662160, 1162000, 395000]}}

  "ELY" {
  ;; Pohjois-Pohjanmaa, Pohjois-Savo, Varsinais-Suomi, Uudenmaa, Etelä-Pohjanmaa, Kaakkois-Suomi, Keski-Suomi, Lappi, Pirkanmaa
         :organisaatiot [23, 20, 17, 16, 22, 18, 21, 24, 19],
         :asukasmaara   [120000, 100000, 150000, 320000, 1535000, 150000, 230000, 234666, 200000],
         :haetut {
                         2010 [4726300, 8946000, 5044972, 5949922, 3589635, 3940058, 3425700, 3416000, 2988258],
                         2011 [6190000, 10225000, 5885000, 8301474, 4308816, 4419818, 4405000, 3980000, 4392405],
                         2012 [5740000, 9525222, 6900000, 11100000, 4340000, 5440000, 3960000, 3990000, 4120000],
                         2013 [6026000, 7826094, 6985000, 6461517, 4113803, 1450669, 2535000, 4165000, 3762000],
                         2014 [4983361, 6775000, 2980000, 9590919, 3664548, 1705803, 1764000, 3987000, 2415000]}
         :myonnetyt {
                         2010 [4577000, 8653000, 4756000, 6272000, 3264000, 3539000, 3186000, 3284000, 3073000],
                         2011 [4423000, 8594000, 4802000, 6225000, 3579000, 3662000, 3183000, 3293000, 3036000],
                         2012 [4209000, 8250000, 4803000, 6303000, 3507000, 3457000, 3003000, 3324000, 3141000],
                         2013 [4100000, 6900000, 2800000, 4600000, 3450000, 1450000, 1900000, 3350000, 2200000],
                         2014 [3879000, 6695000, 2586000, 5095000, 3192000, 3065000, 1684000, 3138000, 1985000]}}})

(def avustus-data+all
  (assoc avustus-data-2010-2015 "ALL" (apply m/deep-merge-with #(apply concat %) (vals avustus-data-2010-2015))))

(def sum (partial reduce +))

(defn data->avustus-tilasto [data]
  (concat (map #(cons "H" %) (m/map-values sum (:haetut data)))
          (map #(cons "M" %) (m/map-values sum (:myonnetyt data)))))

(def avustus-tilasto-2010-2015
  (m/map-values data->avustus-tilasto avustus-data+all))

(defn data->avustus-tilasto-organisaatio [data]
  (mapcat #(map vector
             (:organisaatiot data)
             (repeat %)
             (get (:haetut data) %)
             (get (:myonnetyt data) %))
       (range 2010 2015)))

(def avustus-tilasto-organisaatio-2010-2015
  (m/map-values data->avustus-tilasto-organisaatio avustus-data+all))

(defn data->avustus-asukastakohti [data]
  (mapcat #(map vector
                (:organisaatiot data)
                (repeat %)
                (map (fn [myonnetty asukasmaara] (/ myonnetty asukasmaara))
                     (get (:myonnetyt data) %) (:asukasmaara data)))
          (range 2010 2015)))

(def avustus-asukastakohti-2010-2015
  (m/map-values data->avustus-asukastakohti avustus-data+all))

(defn include-old-data [old-data new-data]
  (cons (first new-data) ;; header
    (concat old-data (rest new-data))))

(defn avustus-tilasto [organisaatiolajitunnus]
  (include-old-data
    (get avustus-tilasto-2010-2015 organisaatiolajitunnus)
    (select-avustus-ah0-group-by-vuosi (c/bindings->map organisaatiolajitunnus)
                                       {:as-arrays? true :connection db})))

(defn avustus-tilasto-group-by-organisaatio [organisaatiolajitunnus]
  (include-old-data
    (get avustus-tilasto-organisaatio-2010-2015 organisaatiolajitunnus)
    (select-avustus-ah0-group-by-organisaatio-vuosi (c/bindings->map organisaatiolajitunnus)
                                                    {:as-arrays? true :connection db})))

(defn avustus-asukastakohti-group-by-organisaatio [organisaatiolajitunnus]
  (include-old-data
    (get avustus-asukastakohti-2010-2015 organisaatiolajitunnus)
    (select-avustus-asukastakohti-ah0-group-by-organisaatio-vuosi (c/bindings->map organisaatiolajitunnus)
                                                                  {:as-arrays? true :connection db})))