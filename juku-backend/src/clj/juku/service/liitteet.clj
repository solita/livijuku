(ns juku.service.liitteet
  (:require [juku.db.yesql-patch :as sql]
            [juku.db.database :refer [db with-transaction]]
            [juku.db.coerce :as coerce]
            [schema.coerce :as scoerce]
            [juku.service.hakemus-core :as hc]
            [ring.util.http-response :as r]
            [clojure.string :as str]
            [slingshot.slingshot :as ss]
            [juku.schema.liitteet :as s]
            [juku.settings :refer [settings]]
            [clojure.java.io :as io])
  (:import (java.io InputStream File)))

(def constraint-errors
  {:liite_pk {:http-response r/conflict :message "Kaksi eri käyttäjää on lisännyt liitteen samanaikaisesti."}
   :liite_hakemus_fk {:http-response r/not-found :message "Liitteen hakemusta (id = {hakemusid}) ei ole olemassa."}})

(sql/defqueries "liitteet.sql" {:constraint-errors constraint-errors
                                :dissoc-error-params [:sisalto]})

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
    (let [hakemus (hc/get-any-hakemus (:hakemusid liite) select-hakemus-for-update)]
      (hc/assert-edit-hakemus-content-allowed*! hakemus)
      (insert-liite! (assoc liite :sisalto sisalto))
      (assert-liite-maxsize! (:hakemusid liite) (:liite-max-size settings))))
  nil)

(defn add-liite-from-file! [liite ^File sisalto]
  (if (> (.length sisalto) 0)
    (add-liite! liite (io/input-stream sisalto))
    (ss/throw+ {:http-response r/bad-request
                :message "Liitteen sisältö ei saa olla tyhjä. Liitteen koko pitää olla enemmän kuin 0 tavua."})))

(defn delete-liite! [hakemusid liitenumero]
  (hc/assert-edit-hakemus-content-allowed*! (hc/get-hakemus hakemusid))
  (update-liite-set-poistoaika! {:hakemusid hakemusid :liitenumero liitenumero})
  nil)

(defn update-liite-nimi! [hakemusid liitenumero nimi]
  (hc/assert-edit-hakemus-content-allowed*! (hc/get-hakemus hakemusid))
  (update-liite-set-nimi! {:hakemusid hakemusid :liitenumero liitenumero :nimi nimi})
  nil)

(defn find-liite-sisalto [hakemusid liitenumero]
  (hc/assert-view-hakemus-content-allowed*! (hc/get-hakemus hakemusid))
  (if-let [liite (first (select-liite-sisalto {:hakemusid hakemusid :liitenumero liitenumero}))]
    (update-in liite [:sisalto] coerce/inputstream)))

(defn liitteet-section [hakemusid]
  (let [liitteet (find-liitteet hakemusid)
        nimet (map (comp (partial str "\t") :nimi) liitteet)]
    (str/join "\n" nimet)))