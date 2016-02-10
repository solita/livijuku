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

;; In general tunnusluku field sql-part is a function of group-by and where parameters
(defn pivoted-fields [filter-id value->column-name]
  (fn [where _]
    (if-let [filter-value (filter-id where)]
      (value->column-name filter-value)
      [(hsql/raw (str/join " + "(map name (vals value->column-name)))) :tunnusluku])))

(def select
  {:nousut                 (constantly :nousut)
   :lahdot                 (constantly :lahdot)
   :linjakilometrit        (constantly :linjakilometrit)
   :nousut-viikko          (constantly :nousut)
   :lahdot-viikko          (constantly :lahdot)
   :linjakilometrit-viikko (constantly :linjakilometrit)
   :liikennointikorvaus    (constantly :korvaus)
   :lipputulo              (pivoted-fields
                             :lipputuloluokkatunnus
                             {"KE" :kertalipputulo "AR" :arvolipputulo "KA" :kausilipputulo "--" :lipputulo})
   :kalusto                (constantly :lukumaara)
   :kustannukset
   :lippuhinnat})

(defn tunnusluku-tilasto [tunnusluku organisaatiolajitunnus where group-by]
  )


