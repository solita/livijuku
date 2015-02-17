(ns juku.service.test
  (:require [juku.db.database :refer [db]]
            [yesql.core :as sql]))

(sql/defqueries "juku/service/test.sql" {:connection db})

(defn find-next-notcreated-hakemuskausi []
  (+ (:next (first (select-max-vuosi-from-hakemuskausi))) 1))