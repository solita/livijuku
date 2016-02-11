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
            [common.collection :as coll]))


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
   :lippuhinnat            :hinta})

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
   :lippuhinnat            :fact_lippuhinta_unpivot_view})

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


