(ns juku.db.hakemus
  (:require [yesql.core :as sql]
            [clojure.java.jdbc :as jdbc]
            [juku.db.tietokanta :refer [db]]
            [juku.db.coerce :as coerce]
            [schema.coerce :as scoerce]
            [juku.schema.hakemus :refer :all]))


(sql/defqueries "hakemus.sql" {:connection db})

(defn hakemus-coercien-matcher [schema]
  (or
    (coerce/timestamp->localdate-matcher schema)))

(def coerce-hakemus (scoerce/coercer Hakemus hakemus-coercien-matcher))

(defn find-osaston-hakemukset [osastoid]
  (map (comp coerce-hakemus coerce/transform-row)
    (select-osaston-hakemukset {:osastoid osastoid})))


