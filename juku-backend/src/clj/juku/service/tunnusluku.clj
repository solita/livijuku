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
  {:fact_liikenne_pk
     {:http-response r/bad-request
      :message       "Liikennetilasto {vuosi}-{kuukausi}-{sopimustyyppitunnus} on jo olemassa organisaatiolle: {organisaatioid}."}
   :fact_liikenne_organisaatio_fk
     {:http-response r/not-found
      :message       "Liikennetilastoa {vuosi}-{sopimustyyppitunnus} ei voi tallentaa, koska organisaatiota {organisaatioid} ei ole olemassa."}
   :fact_liikenne_sopimustyyppi_fk
     {:http-response r/not-found
      :message       "Sopimustyyppiä {sopimustyyppitunnus} ei ole olemassa."}})

(def liikenneviikko-constraint-errors
  {:fact_liikenneviikko_pk
     {:http-response r/bad-request
      :message       "Liikenneviikkotilasto {vuosi}-{viikonpaivaluokkatunnus}-{sopimustyyppitunnus} on jo olemassa organisaatiolle: {organisaatioid}."}
   :fact_liikenneviikko_organisaatio_fk
     {:http-response r/not-found
      :message       "Liikenneviikkotilastoa {vuosi}-{sopimustyyppitunnus} ei voi tallentaa, koska organisaatiota {organisaatioid} ei ole olemassa."}
   :fact_liikenneviikko_sopimustyyppi_fk
     {:http-response r/not-found
      :message       "Sopimustyyppiä {sopimustyyppitunnus} ei ole olemassa."}})

; *** Tunnulukuihin liittyvät kyselyt ***
(sql/defqueries "tunnusluku.sql" {:constraint-errors (merge liikenne-constraint-errors
                                                            liikenneviikko-constraint-errors)})

; *** Tunnuslukujen toiminnot ***

(defn liikennevuosi-id [vuosi organisaatioid sopimustyyppitunnus]
  {:vuosi vuosi
   :organisaatioid organisaatioid
   :sopimustyyppitunnus sopimustyyppitunnus})

; *** Liikenteen kysyntä ja tarjonta - kuukausitilastot ***

(defn find-liikennevuositilasto [vuosi organisaatioid sopimustyyppitunnus]
    (map coerce-liikennesuorite (select-liikennevuositilasto (liikennevuosi-id vuosi organisaatioid sopimustyyppitunnus))))

(defn init-liikennevuosi! [vuosi organisaatioid sopimustyyppitunnus]
  (ss/try+
    (insert-default-liikennevuosi-if-not-exists! (liikennevuosi-id vuosi organisaatioid sopimustyyppitunnus))
    (catch [:violated-constraint "FACT_LIIKENNE_PK"] {}))
  nil)

(defn save-liikennevuositilasto! [vuosi organisaatioid sopimustyyppitunnus liikennekuukaudet]

  (with-transaction
    (init-liikennevuosi! vuosi organisaatioid sopimustyyppitunnus)
    (let [id (liikennevuosi-id vuosi organisaatioid sopimustyyppitunnus)
          batch (sort-by :kuukausi liikennekuukaudet)]
      (dml/update-batch-where! db "fact_liikenne"
                               (map (c/partial-first-arg dissoc :kuukausi) batch)
                               (map (partial merge id) (map (c/partial-first-arg select-keys [:kuukausi]) batch)))))
  nil)

; *** Liikenteen kysyntä ja tarjonta - keskimääräisen talviviikon päivittäinen liikenne ***

(defn find-liikenneviikkotilasto [vuosi organisaatioid sopimustyyppitunnus]
  (map coerce-liikennesuorite (select-liikennevuositilasto (liikennevuosi-id vuosi organisaatioid sopimustyyppitunnus))))

(defn init-liikenneviikko! [vuosi organisaatioid sopimustyyppitunnus]
  (ss/try+
    (insert-default-liikenneviikko-if-not-exists! (liikennevuosi-id vuosi organisaatioid sopimustyyppitunnus))
    (catch [:violated-constraint "FACT_LIIKENNEVIIKKO_PK"] {}))
  nil)

(defn save-liikenneviikkotilasto! [vuosi organisaatioid sopimustyyppitunnus liikenneviikko]

  (with-transaction
    (init-liikenneviikko! vuosi organisaatioid sopimustyyppitunnus)
    (let [id (liikennevuosi-id vuosi organisaatioid sopimustyyppitunnus)
          batch (sort-by :viikonpaivaluokkatunnus liikenneviikko)]
      (dml/update-batch-where! db "fact_liikenneviikko"
                               (map (c/partial-first-arg dissoc :viikonpaivaluokkatunnus) batch)
                               (map (partial merge id) (map (c/partial-first-arg select-keys [:viikonpaivaluokkatunnus]) batch)))))
  nil)