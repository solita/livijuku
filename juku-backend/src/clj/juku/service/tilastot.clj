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
            [clojure.java.jdbc :as jdbc]))


; *** Tilastoihin liittyvät kyselyt ***
(sql/defqueries "tilastot.sql")

(defn find-nousut [] (select-nousut {} {:as-arrays? true :connection db}))


