(ns juku.service.ely-hakemus
  (:require [juku.db.yesql-patch :as sql]
            [juku.db.database :refer [db with-transaction]]
            [juku.db.coerce :as coerce]
            [juku.db.sql :as dml]
            [juku.service.hakemus-core :as hc]
            [juku.schema.ely-hakemus :as s]
            [slingshot.slingshot :as ss]
            [ring.util.http-response :as r]))

; *** ELY-hakemuksen tietoihin liittyvät kyselyt ***
(sql/defqueries "ely.sql")

; *** ELY-hakemus-skeemaan liittyvät konversiot tietokannan tietotyypeistä ***
(def coerce-maararahatarve (coerce/coercer s/Maararahatarve))
(def coerce-kehityshanke (coerce/coercer s/Kehityshanke))

; *** Virheviestit tietokannan rajoitteista ***
(def maararahatarve-constraint-errors
  {:maararahatarve_pk {:http-response r/bad-request
                        :message "Määrärahatarve {maararahatarvetyyppitunnus} on jo olemassa hakemuksella (id = {hakemusid})."}
   :maararahatarve_hakemus_fk {:http-response r/not-found
                                :message "Määrärahatarpeen {maararahatarvetyyppitunnus} hakemusta (id = {hakemusid}) ei ole olemassa."}
   :mrtarve_mrtarvetyyppi_fk {:http-response r/not-found
                              :message "Määrärahatarvetyyppiä {maararahatarvetyyppitunnus} ei ole olemassa."}})

(def kehityshanke-constraint-errors
  {:kehityshanke_pk {:http-response r/bad-request
                     :message "Kehityshanke {numero} on jo olemassa hakemuksella (id = {hakemusid})."}
   :kehityshanke_hakemus_fk {:http-response r/not-found
                             :message "Kehityshankkeen {numero} hakemusta (id = {hakemusid}) ei ole olemassa."}})

; *** Määrärahatarpeiden toiminnot ***
(defn update-maararahatarve [hakemusid maararahatarve]
  (dml/update-where! db "maararahatarve"
                     (dissoc maararahatarve :maararahatarvetyyppitunnus)
                     (assoc (select-keys maararahatarve [:maararahatarvetyyppitunnus]) :hakemusid hakemusid)))

(defn save-maararahatarpeet! [hakemusid maararahatarpeet]
  (hc/assert-edit-hakemus-content-allowed*! (hc/get-hakemus hakemusid))

  (with-transaction
    (doseq [maararahatarve maararahatarpeet]
      (dml/assert-update
        (update-maararahatarve hakemusid maararahatarve)
        (ss/throw+ {:http-response r/not-found
                    :message (str "Määrärahatarvetyyppiä " (:maararahatarvetyyppi maararahatarve)
                                  " ei ole olemassa hakemukselle: " hakemusid)}))))
  nil)

(defn find-hakemus-maararahatarpeet [hakemusid]
  (hc/assert-view-hakemus-content-allowed*! (hc/get-hakemus hakemusid))

  (map coerce-maararahatarve (select-hakemus-maararahatarve {:hakemusid hakemusid})))

(defn find-maararahatarvetyypit [] (select-maararahatarvetyypit))

; *** Kehityshankkeiden toiminnot ***
(defn- insert-kehityshanke [kehityshanke]
  (:id (dml/insert db "kehityshanke" kehityshanke kehityshanke-constraint-errors kehityshanke)))

(defn save-kehityshankkeet! [hakemusid kehityshankkeet]
  (hc/assert-edit-hakemus-content-allowed*! (hc/get-hakemus hakemusid))

  (with-transaction
    (delete-hakemus-kehityshanke! {:hakemusid hakemusid})
    (doseq [kehityshanke kehityshankkeet]
      (insert-kehityshanke (assoc kehityshanke :hakemusid hakemusid))))
  nil)

(defn find-hakemus-kehityshankkeet [hakemusid]
  (hc/assert-view-hakemus-content-allowed*! (hc/get-hakemus hakemusid))

  (map coerce-kehityshanke (select-hakemus-kehityshanke {:hakemusid hakemusid})))