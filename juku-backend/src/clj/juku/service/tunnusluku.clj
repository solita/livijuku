(ns juku.service.tunnusluku
  (:require [juku.db.yesql-patch :as sql]
            [juku.db.coerce :as coerce]
            [juku.schema.tunnusluku :as s]
            [juku.db.database :refer [db with-transaction]]
            [ring.util.http-response :as r]
            [juku.db.sql :as dml]
            [slingshot.slingshot :as ss]
            [common.core :as c]
            [clojure.string :as str]
            [common.string :as strx]
            [common.map :as m]
            [common.collection :as coll]
            [juku.service.organisaatio :as org]))

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
(create-crud-operations "lipputulo" s/Lipputulo "fact_lipputulo" :kuukausi)
(create-crud-operations "liikennointikorvaus" s/Liikennointikorvaus "fact_liikennointikorvaus" :kuukausi)

; *** Alueen tiedot ***

(def coerce-alue (coerce/coercer s/Alue))

(defn find-alue [vuosi organisaatioid]
  (first (map (comp coerce-alue coerce/row->object) (select-alue {:vuosi vuosi :organisaatioid organisaatioid}))))

(defn- insert-alue! [vuosi organisaatioid alue]
  (let [data (assoc alue :vuosi vuosi :organisaatioid organisaatioid )]
    (dml/insert db "fact_alue" (coerce/object->row data) alue-constraint-errors data)))

(defn- update-alue! [vuosi organisaatioid alue]
  (dml/update-where! db "fact_alue" (coerce/object->row alue) {:vuosi vuosi :organisaatioid organisaatioid}))

(defn save-alue! [vuosi organisaatioid alue]
  (dml/upsert update-alue! insert-alue! vuosi organisaatioid alue))

; *** Lippuhinta tiedot ***

(def coerce-lippuhinta (coerce/coercer s/Lippuhinta))

(defn find-lippuhinnat [vuosi organisaatioid]
  (map coerce-lippuhinta (select-lippuhinta {:vuosi vuosi :organisaatioid organisaatioid})))

(defn init-lippuhinta! [vuosi organisaatioid]
  (dml/insert-ignore-unique-constraint-error insert-default-lippuhinta-if-not-exists!
                                             {:vuosi vuosi :organisaatioid organisaatioid}))

(defn save-lippuhinnat! [vuosi organisaatioid data]
  (with-transaction
    (init-lippuhinta! vuosi organisaatioid)
    (let [id {:vuosi vuosi :organisaatioid organisaatioid}
          batch (sort-by :vyohykemaara data)]
      (dml/update-batch-where! db "fact_lippuhinta"
                               (map (c/partial-first-arg dissoc :vyohykemaara) batch)
                               (map (partial merge id) (map (c/partial-first-arg select-keys [:vyohykemaara]) batch))))))

; *** kommentit ***

(defn find-kommentti [vuosi organisaatioid sopimustyyppitunnus]
  (first (map (comp (c/nil-safe coerce/clob->string) :kommentti)
              (select-kommentti (liikennevuosi-id vuosi organisaatioid sopimustyyppitunnus)))))

(defn- insert-kommentti! [vuosi organisaatioid sopimustyyppitunnus kommentti]
  (let [data (assoc (liikennevuosi-id vuosi organisaatioid sopimustyyppitunnus) :kommentti kommentti)]
    (dml/insert db "tunnuslukukommentti" data alue-constraint-errors data)))

(defn- update-kommentti! [vuosi organisaatioid sopimustyyppitunnus kommentti]
  (dml/update-where! db "tunnuslukukommentti" {:kommentti kommentti}
                     (liikennevuosi-id vuosi organisaatioid sopimustyyppitunnus)))

(defn save-kommentti! [vuosi organisaatioid sopimustyyppitunnus kommentti]
  (dml/upsert update-kommentti! insert-kommentti! vuosi organisaatioid sopimustyyppitunnus kommentti))

; *** csv lataus (konversiodata excel:stä) ***

(defn parse-csv [csv]
  (let [lines (str/split csv #"\n")]
    (map (c/partial-first-arg str/split #";") lines)))

(def kuukaudet
  (merge
    (m/zip-object
      (map (c/partial-first-arg str "kuussa") ["tammi" "helmi" "maalis" "huhti" "touko" "kesä" "heinä" "elo" "syys" "loka" "marras" "joulu"])
      (range 1 13))
    (m/zip-object
      (map (c/partial-first-arg str "kuu") ["tammi" "helmi" "maalis" "huhti" "touko" "kesä" "heinä" "elo" "syys" "loka" "marras" "joulu"])
      (range 1 13))))

(def sopimustyypit {"brutto" "BR"
                    "kos" "KOS"
                    "siirtymäajan liikenne" "SA"
                    "me" "ME"})

(def viikonpaivaluokat
  {"arkipäivän" "A"
   "arkivuorokauden" "A"
   "lauantain" "LA"
   "sunnuntain" "SU"})

(defn parse-kk [otsikko]
  (c/if-let* [sopimustyyppitunnus (sopimustyypit (get otsikko 1))
              kuukausi (kuukaudet (get otsikko 2))]
            (c/bindings->map sopimustyyppitunnus kuukausi)
             nil))

(defn parse-viikonpaivaluokka [otsikko]
  (c/if-let* [sopimustyyppitunnus (sopimustyypit (get otsikko 1))
              viikonpaivaluokkatunnus (viikonpaivaluokat (get otsikko 2))]
             (c/bindings->map sopimustyyppitunnus viikonpaivaluokkatunnus)
             nil))

(def tunnuslukuotsikot
  {
   #"(\S*): nousijat (\S*)" [:liikennevuositilasto :nousut parse-kk]
   #"(\S*): linja-kilometrit (\S*)" [:liikennevuositilasto :linjakilometrit parse-kk]
   #"(\S*): lähdöt (\S*)" [:liikennevuositilasto :lahdot parse-kk]

   #"(\S*): (\S*) keskimääräinen nousumäärä.*" [:liikenneviikkotilasto :nousut parse-viikonpaivaluokka]
   #"(\S*): talviliikenteen keskimääräisen (\S*) tarjonta" [:liikenneviikkotilasto :linjakilometrit parse-viikonpaivaluokka]
   #"(\S*): talviliikenteen keskimääräisen (\S*) lähtömäärä" [:liikenneviikkotilasto :lahdot parse-viikonpaivaluokka]

   #"(\S*): Kertalipuista saadut lipputulot (\S*)" [:lipputulo :kertalipputulo parse-kk]
   #"(\S*): Arvolipuista saadut lipputulot (\S*)" [:lipputulo :arvolipputulo parse-kk]
   #"(\S*): Kausilipuista saadut lipputulot (\S*)" [:lipputulo :kausilipputulo parse-kk]
   #"(\S*): Lipputulot (\S*)" [:lipputulo :lipputulo parse-kk]

   #"(\S*): Maksettu liikennöintikorvaus (\S*)" [:liikennointikorvaus :korvaus parse-kk]
   #"(\S*): Maksettu liikennöintikorvaus ja lipputuki (\S*)" [:liikennointikorvaus :korvaus parse-kk]
   #"Maksettu liikennöinnin nousukorvaus (\(KOS\)) - (\S*)" [:liikennointikorvaus :nousukorvaus parse-kk]
   #"(ME): Asiakashinnan mukaiset nousukorvaukset (\S*)" [:liikennointikorvaus :korvaus parse-kk]
   })

(defn parse-tunnusluku [data]
  (coll/find-first c/not-nil?
    (map (fn [[key value]]
             (c/if-let* [match (re-matches key (str/lower-case (:tunnusluku data)))
                         tunnusluku ((c/third value) match)]
                         (assoc tunnusluku
                           :tunnuslukutyyppi (first value)
                           :vuosi (:vuosi data)
                           :organisaatioid (:organisaatioid data)
                           (second value) (:value data))
                         nil))
              tunnuslukuotsikot)))


(defn find-organisaatio [nimi]
  (coll/find-first (coll/eq (comp str/lower-case :nimi) nimi) (org/organisaatiot)))

(defn unpivot-organizations [headers row]
  (let [vuosi+tunnusluku (vec (take 2 row))
        org-names (map (comp str/trim str/lower-case) (drop 2 headers))
        org-data (drop 2 row)]
    (map-indexed (fn [idx value]
                   (conj vuosi+tunnusluku (:id (find-organisaatio (nth org-names idx))) value))
                 org-data)))

(def tunnusluku-pivot
  {
   :liikennevuositilasto  :kuukausi
   :liikenneviikkotilasto :viikonpaivaluokkatunnus
   :lipputulo             :kuukausi
   :liikennointikorvaus   :kuukausi
   })

(def tunnusluku-save
  {
   :liikennevuositilasto  save-liikennevuositilasto!
   :liikenneviikkotilasto save-liikenneviikkotilasto!
   :lipputulo             save-lipputulo!
   :liikennointikorvaus   save-liikennointikorvaus!
   })

(defn process-data [pivot data]
  (let [result (map (c/partial-first-arg dissoc :tunnuslukutyyppi :vuosi :organisaatioid :sopimustyyppitunnus) data)]
    (if (nil? pivot)
      result
      (map (partial apply merge) (vals (group-by pivot result))))))

(defn parse-vuosi [txt]
  (try (Integer/parseInt txt) (catch Throwable _ nil)))

(defn import-csv [csv]
  (let [data (parse-csv csv)
        headers (map str/lower-case (first data))
        tunnusluvut (->> (rest data)
                         (map (c/partial-first-arg update 0 parse-vuosi))
                         (filter (comp c/not-nil? first))
                         (filter (comp strx/not-blank? second))
                         (mapcat (partial unpivot-organizations headers))
                         (map (partial m/zip-object [:vuosi :tunnusluku :organisaatioid :value]))
                         (map parse-tunnusluku)
                         (filter (comp c/not-nil? :tunnuslukutyyppi))
                         (group-by (juxt :tunnuslukutyyppi :vuosi :organisaatioid :sopimustyyppitunnus)))]

    (doseq [[[tunnuslukutyyppi vuosi organisaatioid sopimustyyppitunnus] data] tunnusluvut]
      (let [save (tunnusluku-save tunnuslukutyyppi)
            pivot (tunnusluku-pivot tunnuslukutyyppi)]
        (if (nil? sopimustyyppitunnus)
          (save vuosi organisaatioid (process-data pivot data))
          (save vuosi organisaatioid sopimustyyppitunnus (process-data pivot data)))))))

