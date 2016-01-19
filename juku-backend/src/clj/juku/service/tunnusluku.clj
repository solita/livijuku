(ns juku.service.tunnusluku
  (:require [juku.db.yesql-patch :as sql]
            [juku.db.coerce :as coerce]
            [juku.schema.tunnusluku :as s]
            [juku.db.database :refer [db with-transaction]]
            [ring.util.http-response :as r]
            [juku.db.sql :as dml]
            [slingshot.slingshot :as ss]
            [common.core :as c]
            [clojure.string :as str]))

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

(def alue-constraint-errors
  {:fact_alue_pk
   {:http-response r/bad-request
    :message       "Aluetiedot vuodelle {vuosi} on jo olemassa organisaatiolle: {organisaatioid}."}
   :fact_alue_organisaatio_fk
   {:http-response r/not-found
    :message       "Aluetietoje vuodelle {vuosi} ei voi tallentaa, koska organisaatiota {organisaatioid} ei ole olemassa."}})

; *** Tunnulukuihin liittyvät kyselyt ***
(sql/defqueries "tunnusluku.sql" {:constraint-errors (merge liikenne-constraint-errors
                                                            liikenneviikko-constraint-errors)})

; *** Tunnuslukujen yleiset funktiot ***

(defn liikennevuosi-id [vuosi organisaatioid sopimustyyppitunnus]
  {:vuosi vuosi
   :organisaatioid organisaatioid
   :sopimustyyppitunnus sopimustyyppitunnus})

(defmacro create-crud-operations [tunnusluvunnimi schema fact-tablename & extra-dimensions]
  (let [find (symbol (str "find-" tunnusluvunnimi))
        coerce (symbol (str "coerce-" (str/lower-case (name schema))))
        select (symbol (str "select-" tunnusluvunnimi))
        init (symbol (str "init-" tunnusluvunnimi "!"))
        insert-default (symbol (str "insert-default-" tunnusluvunnimi "-if-not-exists!"))
        save (symbol (str "save-" tunnusluvunnimi "!"))]
    `(do
        (def ~coerce (coerce/coercer ~schema))

        (defn ~find [~'vuosi ~'organisaatioid ~'sopimustyyppitunnus]
          (map ~coerce (~select (liikennevuosi-id ~'vuosi ~'organisaatioid ~'sopimustyyppitunnus))))

        (defn ~init [~'vuosi ~'organisaatioid ~'sopimustyyppitunnus]
          (ss/try+
            (~insert-default (liikennevuosi-id ~'vuosi ~'organisaatioid ~'sopimustyyppitunnus))
            (catch [:violated-constraint (str ~fact-tablename "_PK")] {}))
          nil)

        (defn ~save [~'vuosi ~'organisaatioid ~'sopimustyyppitunnus ~'data]

          (with-transaction
            (~init ~'vuosi ~'organisaatioid ~'sopimustyyppitunnus)
            (let [id# (liikennevuosi-id ~'vuosi ~'organisaatioid ~'sopimustyyppitunnus)
                  batch# (sort-by ~(first extra-dimensions) ~'data)]
              (dml/update-batch-where! db ~fact-tablename
                                       (map (c/partial-first-arg dissoc ~@extra-dimensions) batch#)
                                       (map (partial merge id#) (map (c/partial-first-arg select-keys '~extra-dimensions) batch#)))))
          nil))))

; *** Tunnuslukujen crud-funktioiden generointi ***

(create-crud-operations "liikennevuositilasto" s/Liikennekuukausi "fact_liikenne" :kuukausi)
(create-crud-operations "liikenneviikkotilasto" s/Liikennepaiva "fact_liikenneviikko" :viikonpaivaluokkatunnus)
(create-crud-operations "kalusto" s/Kalusto "fact_kalusto" :paastoluokkatunnus)
(create-crud-operations "lipputulo" s/Lipputulo "fact_lipputulo" :kuukausi :lippuluokkatunnus)
(create-crud-operations "liikennointikorvaus" s/Liikennointikorvaus "fact_liikennointikorvaus" :kuukausi)
(create-crud-operations "lippuhinta" s/Lippuhinta "fact_lippuhinta" :lippuluokkatunnus :vyohykkelukumaara)

; *** Alueen tiedot ***

(def coerce-alue (coerce/coercer s/Alue))

(defn find-alue [vuosi organisaatioid]
  (map (comp coerce-alue coerce/row->object) (select-alue {:vuosi vuosi :organisaatioid organisaatioid})))

(defn- insert-alue! [vuosi organisaatioid alue]
  (let [data (assoc alue :vuosi vuosi :organisaatioid organisaatioid )]
    (dml/insert db "fact_alue" (coerce/object->row data) alue-constraint-errors data)))

(defn- update-alue! [vuosi organisaatioid alue]
  (dml/update-where! db "fact_alue" (coerce/object->row alue) {:vuosi vuosi :organisaatioid organisaatioid}))

(defn save-alue! [vuosi organisaatioid alue]
  (dml/upsert update-alue! insert-alue! vuosi organisaatioid alue))