(ns juku.service.liitteet
  (:require [yesql.core :as sql]
            [juku.db.database :refer [db]]
            [juku.db.coerce :as coerce]
            [schema.coerce :as scoerce]
            [juku.schema.liitteet :as s])
  (:import (java.io InputStream)))

(sql/defqueries "liitteet.sql" {:connection db})

(def coerce-liite (scoerce/coercer s/Liite coerce/db-coercion-matcher))

(defn find-liitteet [hakemusid]
  (map coerce-liite (select-liitteet {:hakemusid hakemusid})))

(defn add-liite! [liite ^InputStream sisalto]
  (insert-liite! (assoc liite :sisalto sisalto))
  nil)

(defn delete-liite [hakemusid liitenumero]
  (update-liite-set-poistoaika! {:hakemusid hakemusid :liitenumero liitenumero})
  nil)