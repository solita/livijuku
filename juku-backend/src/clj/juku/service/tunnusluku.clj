(ns juku.service.tunnusluku
  (:require [juku.db.yesql-patch :as sql]
            [juku.db.coerce :as coerce]
            [juku.schema.tunnusluku :as s]
            [juku.db.database :refer [db with-transaction]]
            [ring.util.http-response :as r]
            [juku.db.sql :as dml]
            [slingshot.slingshot :as ss]
            [common.core :as c]))

; *** Seuranta-skeemaan liittyvät konversiot tietokannan tietotyypeistä ***
(def coerce-liikennesuorite (coerce/coercer s/Liikennekuukausi))

; *** Virheviestit tietokannan rajoitteista ***
(def liikenne-constraint-errors
  {:fact_liikenne_pk {:http-response r/bad-request
                      :message       "Liikennetilasto {vuosi}-{kuukausi}-{sopimustyyppitunnus} on jo olemassa organisaatiolle: {organisaatioid}."}
   :fact_liikenne_organisaatio_fk {:http-response r/not-found
                                   :message       "Liikennetilastoa {vuosi}-{sopimustyyppitunnus} ei voi tallentaa, koska organisaatiota {organisaatioid} ei ole olemassa."}
   :fact_liikenne_sopimustyyppi_fk {:http-response r/not-found
                                    :message       "Sopimustyyppiä {sopimustyyppitunnus} ei ole olemassa."}})

; *** Tunnulukuihin liittyvät kyselyt ***
(sql/defqueries "tunnusluku.sql" {:constraint-errors liikenne-constraint-errors})

; *** Tunnuslukujen toiminnot ***

(defn liikennevuosi-id [vuosi organisaatioid sopimustyyppitunnus]
  {:vuosi vuosi
   :organisaatioid organisaatioid
   :sopimustyyppitunnus sopimustyyppitunnus})

(defn find-liikennevuositilasto [vuosi organisaatioid sopimustyyppitunnus]
    (map coerce-liikennesuorite (select-liikennevuositilasto (liikennevuosi-id vuosi organisaatioid sopimustyyppitunnus))))

(defn init-liikennevuosi! [vuosi organisaatioid sopimustyyppitunnus]
  (ss/try+
    (insert-default-liikennevuosi-if-not-exists! (liikennevuosi-id vuosi organisaatioid sopimustyyppitunnus))
    (catch [:violated-constraint "FACT_LIIKENNE_PK"] {}))
  nil)

(defn save-organisaatio-liikennetilasto! [vuosi organisaatioid sopimustyyppitunnus liikennekuukaudet]

  (with-transaction
    (init-liikennevuosi! vuosi organisaatioid sopimustyyppitunnus)
    (let [id {:vuosi vuosi
              :organisaatioid organisaatioid
              :sopimustyyppitunnus sopimustyyppitunnus}
          batch (sort-by :kuukausi liikennekuukaudet)]
      (dml/update-batch-where! db "fact_liikenne"
                               (map (c/partial-first-arg dissoc :kuukausi) batch)
                               (map (partial merge id) (map (c/partial-first-arg select-keys [:kuukausi]) batch)))))
  nil)