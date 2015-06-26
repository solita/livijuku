(ns juku.service.liitteet
  (:require [juku.db.yesql-patch :as sql]
            [juku.db.database :refer [db with-transaction]]
            [juku.db.coerce :as coerce]
            [schema.coerce :as scoerce]
            [ring.util.http-response :as r]
            [slingshot.slingshot :as ss]
            [juku.schema.liitteet :as s]
            [juku.settings :refer [settings]])
  (:import (java.io InputStream)))

(def constraint-errors
  {:liite_pk {:http-response r/conflict :message "Kaksi eri käyttäjää on lisännyt liitteen samanaikaisesti."}
   :liite_hakemus_fk {:http-response r/not-found :message "Liitteen hakemusta (id = {hakemusid}) ei ole olemassa."}})

(sql/defqueries "liitteet.sql" {:constraint-errors constraint-errors})

(def coerce-liite (scoerce/coercer s/Liite+Size coerce/db-coercion-matcher))
(def coerce-liite+ (scoerce/coercer s/Liite+ coerce/db-coercion-matcher))

(defn find-liitteet [hakemusid]
  (map coerce-liite (select-liitteet {:hakemusid hakemusid})))

(defn find-liitteet+sisalto [hakemusid]
  (map (comp coerce-liite+ #(update-in % [:sisalto] coerce/inputstream)) (select-liitteet+sisalto {:hakemusid hakemusid})))

(defn assert-liite-maxsize! [hakemusid maxsize]
  (let [totalsize (:bytesize (first (select-sum-liitekoko {:hakemusid hakemusid})))]
    (if (> totalsize maxsize) (ss/throw+ {:http-response r/request-entity-too-large
                                           :message (str "Hakemuksen " hakemusid
                                                         " liitteiden yhteenlaskettu koko ylittää maksimirajan "
                                                         maxsize)}))))

(defn add-liite! [liite ^InputStream sisalto]
  (with-transaction
    (insert-liite! (assoc liite :sisalto sisalto))
    (assert-liite-maxsize! (:hakemusid liite) (:liite-max-size settings)))
  nil)

(defn delete-liite [hakemusid liitenumero]
  (update-liite-set-poistoaika! {:hakemusid hakemusid :liitenumero liitenumero})
  nil)

(defn update-liite-nimi! [hakemusid liitenumero nimi]
  (update-liite-set-nimi! {:hakemusid hakemusid :liitenumero liitenumero :nimi nimi})
  nil)

(defn find-liite-sisalto [hakemusid liitenumero]
  (if-let [liite (first (select-liite-sisalto {:hakemusid hakemusid :liitenumero liitenumero}))]
    (update-in liite [:sisalto] coerce/inputstream)))