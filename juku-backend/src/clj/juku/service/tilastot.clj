(ns juku.service.tilastot
  (:require [juku.db.yesql-patch :as sql]
            [juku.db.coerce :as coerce]
            [juku.schema.tunnusluku :as s]
            [juku.db.database :refer [db with-transaction]]
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

(def tunnusluvut
  [:nousut :lahdot :linjakilometrit
   :nousut-viikko :lahdot-viikko :linjakilometrit-viikko
   :liikennointikorvaus
   :lipputulo
   :kalusto
   :kustannukset
   :lippuhinnat])

(def join-organisaatio (hsql-h/join :organisaatio [:= :t.organisaatioid :organisaatio.id]))

;; In general select tunnusluku sql-part is a function of group-by and where parameters
;; this is the default case where it only depends on group-by
(defn select-tunnusluku [tunnusluku] (fn [_ group-by] {:select (conj group-by tunnusluku)}))

(defn select-pivoted [filter-id value->column-name]
  (fn [where group-by]
    (if-let [filter-value (filter-id where)]
      {:select (conj group-by (value->column-name filter-value))}
      {:select (conj group-by [(hsql/raw (str/join " + "(map name (vals value->column-name)))) :tunnusluku])})))

(def select
  {:nousut                 (select-tunnusluku :nousut)
   :lahdot                 (select-tunnusluku :lahdot)
   :linjakilometrit        (select-tunnusluku :linjakilometrit)
   :nousut-viikko          (select-tunnusluku :nousut)
   :lahdot-viikko          (select-tunnusluku :lahdot)
   :linjakilometrit-viikko (select-tunnusluku :linjakilometrit)
   :liikennointikorvaus    (select-tunnusluku :korvaus)
   :lipputulo (select-pivoted :lipputuloluokkatunnus
                              {"KE" :kertalipputulo "AR" :arvolipputulo "KA" :kausilipputulo "--" :lipputulo})
   :kalusto   (select-tunnusluku :lukumaara)
   :kustannukset
   :lippuhinnat})

(defn tunnusluku-tilasto [tunnusluku organisaatiolajitunnus where group-by]
  )


(defn find-nousut [] (select-nousut {} {:as-arrays? true :connection db}))


