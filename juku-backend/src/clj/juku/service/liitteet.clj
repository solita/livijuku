(ns juku.service.liitteet
  (:require [yesql.core :as sql]
            [juku.db.database :refer [db]]
            [juku.db.coerce :as coerce]
            [schema.coerce :as scoerce]
            [ring.util.http-response :as r]
            [juku.schema.liitteet :as s])
  (:import (java.io InputStream)
           (java.sql Blob)))

(def constraint-errors
  {:liite_pk {:http-response r/conflict :message "Kaksi eri käyttäjää on lisännyt liitteen samanaikaisesti."}
   :liite_hakemus_fk {:http-response r/not-found :message "Liitteen hakemusta (id = {hakemusid}) ei ole olemassa."}})

(sql/defqueries "liitteet.sql" {:connection db :constraint-errors constraint-errors})

(def coerce-liite (scoerce/coercer s/Liite coerce/db-coercion-matcher))

(defn find-liitteet [hakemusid]
  (map coerce-liite (select-liitteet {:hakemusid hakemusid})))

(defn add-liite! [liite ^InputStream sisalto]
  (insert-liite! (assoc liite :sisalto sisalto))
  nil)

(defn delete-liite [hakemusid liitenumero]
  (update-liite-set-poistoaika! {:hakemusid hakemusid :liitenumero liitenumero})
  nil)

(defn update-liite-nimi! [hakemusid liitenumero nimi]
  (update-liite-set-nimi! {:hakemusid hakemusid :liitenumero liitenumero :nimi nimi})
  nil)

(defn find-liite-sisalto [hakemusid liitenumero]
  (if-let [liite (first (select-liite-sisalto {:hakemusid hakemusid :liitenumero liitenumero}))]
    (update-in liite [:sisalto] #(.getBinaryStream %))))