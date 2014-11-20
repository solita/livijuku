(ns juku.db.hakemus
  (:require [yesql.core :as sql]
            [clojure.java.jdbc :as jdbc]
            [juku.db.tietokanta :as kanta]
            [juku.db.conversion :as db-util]))

(sql/defqueries "hakemus.sql" {:connection kanta/db-connection})

(defmacro defquery [name args & body]
  `(defn ~name ~args
     (db-util/sql-timestamp->joda-datetime ~@body)))

(defquery find-osaston-hakemukset [osastoid] (select-osaston-hakemukset {:osastoid osastoid}))


