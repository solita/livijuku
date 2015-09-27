(ns juku.service.seuranta
  (:require [juku.db.yesql-patch :as sql]
            [juku.db.database :refer [db with-transaction]]
            [juku.db.coerce :as coerce]
            [juku.db.sql :as dml]
            [common.collection :as coll]
            [juku.service.hakemus-core :as hc]
            [juku.schema.seuranta :as s]
            [ring.util.http-response :as r]
            [slingshot.slingshot :as ss]
            [common.collection :as coll]))

; *** Seurantatietoihin liittyvät kyselyt ***
(sql/defqueries "seuranta.sql")

; *** Seuranta-skeemaan liittyvät konversiot tietokannan tietotyypeistä ***
(def coerce-liikennesuorite (coerce/coercer s/Liikennesuorite))

; *** Virheviestit tietokannan rajoitteista ***
(def liikennesuorite-constraint-errors
  {:liikennesuorite_pk {:http-response r/bad-request
                        :message "Liikennesuorite {liikennetyyppitunnus}-{numero} on jo olemassa hakemuksella (id = {hakemusid})."}
   :liikennesuorite_hakemus_fk {:http-response r/not-found
                                :message "Liikennesuoritteen {liikennetyyppitunnus}-{numero} hakemusta (id = {hakemusid}) ei ole olemassa."}
   :liikennesuorite_lnetyyppi_fk {:http-response r/not-found
                                  :message "Liikennetyyppiä {liikennetyyppitunnus} ei ole olemassa."}
   :liikennesuorite_styyppi_fk {:http-response r/not-found
                                :message "Suoritetyyppiä {suoritetyyppitunnus} ei ole olemassa."}})


(defn- insert-liikennesuorite [suorite]
  (:id (dml/insert db "liikennesuorite" suorite liikennesuorite-constraint-errors suorite)))

(defn save-liikennesuoritteet! [hakemusid suoritteet]
  (hc/assert-edit-hakemus-content-allowed*! (hc/get-hakemus hakemusid))

  (when (some (coll/predicate not= :hakemusid hakemusid) suoritteet)
    (ss/throw+ {:http-response r/bad-request
                :message (str "Kaikki suoritteet pitää olla hakemuksesta: " hakemusid)}))

  (with-transaction
    (delete-hakemus-liikennesuorite! {:hakemusid hakemusid})
    (doseq [suorite suoritteet]
      (insert-liikennesuorite suorite)))
  nil)

(defn find-hakemus-liikennesuoritteet [hakemusid]
  (hc/assert-view-hakemus-content-allowed*! (hc/get-hakemus hakemusid))

  (map coerce-liikennesuorite (select-hakemus-liikennesuorite {:hakemusid hakemusid})))

(defn find-suoritetyypit [] (select-suoritetyypit))