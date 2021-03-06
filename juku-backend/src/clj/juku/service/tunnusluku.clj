(ns juku.service.tunnusluku
  (:require [juku.db.yesql-patch :as sql]
            [juku.db.coerce :as coerce]
            [schema.coerce :as sc-coerce]
            [schema.utils :as sc-util]
            [juku.schema.tunnusluku :as s]
            [schema.core :as sc]
            [juku.db.database :refer [db with-transaction]]
            [ring.util.http-response :as r]
            [juku.db.sql :as dml]
            [slingshot.slingshot :as ss]
            [common.core :as c]
            [clojure.string :as str]
            [common.string :as strx]
            [clojure.walk :as walk]
            [common.map :as m]
            [common.collection :as coll]
            [juku.service.organisaatio :as org]
            [common.map :as map]
            [clojure.java.io :as io]
            [clojure-csv.core :as csv]
            [juku.service.common :as common])
  (:import [schema.core Maybe]
           [schema.utils ValidationError]
           (java.io Writer)))

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


; *** Pienten kaupunkiseutujen joukkoliikennetukitiedot ***

(def coerce-joukkoliikennetuki (coerce/coercer s/Joukkoliikennetuki))

(defn find-joukkoliikennetuki  [vuosi organisaatioid]
  (map coerce-joukkoliikennetuki (select-joukkoliikennetuki {:vuosi vuosi :organisaatioid organisaatioid})))

(defn init-joukkoliikennetuki! [vuosi organisaatioid]
  (dml/insert-ignore-unique-constraint-error insert-default-joukkoliikennetuki-if-not-exists!
                                             {:vuosi vuosi :organisaatioid organisaatioid}))

(defn save-joukkoliikennetuki! [vuosi organisaatioid data]
  (if (not-empty data)
    (with-transaction
      (init-joukkoliikennetuki! vuosi organisaatioid)
      (let [id {:vuosi vuosi :organisaatioid organisaatioid}
            batch (sort-by :avustuskohdeluokkatunnus data)]
        (dml/update-batch-where! db "fact_joukkoliikennetuki"
                                 (map (c/partial-first-arg dissoc :avustuskohdeluokkatunnus) batch)
                                 (map (partial merge id) (map (c/partial-first-arg select-keys [:avustuskohdeluokkatunnus]) batch))))
      nil)))

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

(defn parse-int [txt]
  (try (Integer/parseInt txt) (catch Throwable _ nil)))

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

(defn parse-vyohyke [otsikko]
  (if-let [vyohykemaara (parse-int (get otsikko 1))]
          (c/bindings->map vyohykemaara)))

(def tunnuslukuotsikot
  {
   #"(.*): nousijat (\S*)" [:liikennevuositilasto :nousut parse-kk]
   #"(.*): linja-?kilometrit (\S*)" [:liikennevuositilasto :linjakilometrit parse-kk]
   #"(.*): lähdöt (\S*)" [:liikennevuositilasto :lahdot parse-kk]

   #"(.*): (\S*) keskimääräinen nousumäärä.*" [:liikenneviikkotilasto :nousut parse-viikonpaivaluokka]
   #"(.*): talviliikenteen keskimääräisen (\S*) tarjonta.*" [:liikenneviikkotilasto :linjakilometrit parse-viikonpaivaluokka]
   #"(.*): talviliikenteen keskimääräisen (\S*) lähtömäärä.*" [:liikenneviikkotilasto :lahdot parse-viikonpaivaluokka]

   #"(.*): kertalipuista saadut lipputulot (\S*)" [:lipputulo :kertalipputulo parse-kk]
   #"(.*): arvolipuista saadut lipputulot (\S*)" [:lipputulo :arvolipputulo parse-kk]
   #"(.*): kausilipuista saadut lipputulot (\S*)" [:lipputulo :kausilipputulo parse-kk]
   #"(.*): lipputulot (\S*)" [:lipputulo :lipputulo parse-kk]

   #"(.*): maksettu liikennöintikorvaus (\S*)" [:liikennointikorvaus :korvaus parse-kk]
   #"(.*): maksettu liikennöintikorvaus ja lipputuki (\S*)" [:liikennointikorvaus :korvaus parse-kk]
   #"maksettu liikennöinnin nousukorvaus (\(kos\)) - (\S*)" [:liikennointikorvaus :nousukorvaus parse-kk]
   #"(me): asiakashinnan mukaiset nousukorvaukset (\S*)" [:liikennointikorvaus :korvaus parse-kk]

   #"kertalipun hinta, aikuinen, vyöhyke (\d*).*" [:lippuhinta :kertalippuhinta parse-vyohyke]
   #"kausilipun hinta vyöhyke (\d*).*" [:lippuhinta :kausilippuhinta parse-vyohyke]

   #"alueen kuntien lukumäärä" [:alue :kuntamaara (constantly {})]
   #"joukkoliikenteen lippujärjestelmän vyöhykkeiden määrä" [:alue :vyohykemaara (constantly {})]
   #"toimivalta-alueen pysäkkien lukumäärä" [:alue :pysakkimaara (constantly {})]
   #"maapinta-ala, km2" [:alue :maapintaala (constantly {})]
   #"asukasmäärä" [:alue :asukasmaara (constantly {})]
   #"työpaikkamäärä" [:alue :tyopaikkamaara (constantly {})]
   #"suunnittelun ja organisaation henkilöstö, henkilötyövuotta" [:alue :henkilosto (constantly {})]

   #"pendelöivien osuus alueella (oman kunnan ulkopuolella työssäkäynti)" [:alue :pendeloivienosuus (constantly {})]
   #"henkilöautoliikenteen suorite.*" [:alue :henkiloautoliikennesuorite (constantly {})]
   #"autoistumisaste" [:alue :autoistumisaste (constantly {})]
   #"tyytyväisten käyttäjien osuus \(%\)" [:alue :asiakastyytyvaisyys (constantly {})]

   #"ulkoistettujen palveluiden kustannukset asiakaspalvelu" [:alue :kustannus_asiakaspalvelu (constantly {})]
   #"ulkoistettujen palveluiden kustannukset, konsulttipalvelut" [:alue :kustannus_konsulttipalvelu (constantly {})]
   #"ulkoistettujen palveluiden kustannukset , lipunmyyntipalkkiot" [:alue :kustannus_lipunmyyntipalkkio (constantly {})]
   #"ulkoistettujen palveluiden kustannukset , tieto-/maksujärjestelmät" [:alue :kustannus_jarjestelmat (constantly {})]
   #"ulkoistettujen palveluiden kustannukset , muut palvelut" [:alue :kustannus_muutpalvelut (constantly {})]

   })

(defn parse-tunnusluku [data]
  (coll/find-first c/not-nil?
    (map (fn [[key value]]
             (c/if-let* [match (re-matches key (str/trim (str/lower-case (:tunnusluku data))))
                         tunnusluku ((c/third value) match)]
                         (assoc tunnusluku
                           :tunnuslukutyyppi (first value)
                           :vuosi (:vuosi data)
                           :organisaatioid (:organisaatioid data)
                           (second value) (let [value (strx/trim (:value data))]
                                            (when-not (empty? value) value)))
                         nil))
              tunnuslukuotsikot)))


(defn get-organisaatio [organisaatiot nimi]
  (coll/get-first! (coll/eq (comp str/lower-case :nimi) nimi) organisaatiot
                   {:message (str "Organisaatiota: " nimi " ei löydy.")}))

(defn unpivot-organizations [organisaatiot headers row]
  (let [vuosi+tunnusluku (vec (take 2 row))
        org-names (map (comp str/trim str/lower-case) (drop 2 headers))
        org-ids (map (comp :id (partial get-organisaatio organisaatiot)) org-names)
        org-data (drop 2 row)]
    (map-indexed (fn [idx value]
                   (conj vuosi+tunnusluku (nth org-ids idx) value))
                 org-data)))

(def tunnusluku-pivot
  {
   :liikennevuositilasto  :kuukausi
   :liikenneviikkotilasto :viikonpaivaluokkatunnus
   :lipputulo             :kuukausi
   :liikennointikorvaus   :kuukausi
   :lippuhinta            :vyohykemaara
   :alue                  :all
   })

(def tunnusluku-save
  {
   :liikennevuositilasto  save-liikennevuositilasto!
   :liikenneviikkotilasto save-liikenneviikkotilasto!
   :lipputulo             save-lipputulo!
   :liikennointikorvaus   save-liikennointikorvaus!
   :lippuhinta            save-lippuhinnat!
   :alue                  save-alue!
   })

(defn maybe? [type]
  (instance? Maybe type))

(defn keys->optional [schema]
  (walk/postwalk
    (fn [x] (if (and (vector? x)
                     (= (count x) 2)
                     (keyword? (first x))
                     (maybe? (second x)))
              [(sc/optional-key (first x)) (second x)] x))
    schema))

(defn str->bigdec [^String txt]
  (try (BigDecimal. (str/replace (str/replace txt "," ".") #"\h" ""))
    (catch Throwable _ txt)))

(defn str->bigdec-maybe [^String txt]
  (let [num (str->bigdec txt)] (if (= num 0M) nil num)))

(defn to-optional-key [schema key]
  (let [v (schema key)] (-> schema (dissoc key) (assoc (sc/optional-key key) v))))

(def tunnusluku-coercer
  (map/map-values
    (fn [schema] (sc-coerce/coercer
                   (keys->optional schema)
                   (coerce/create-matcher {(sc/maybe sc/Num) 'str->bigdec-maybe
                                           sc/Num 'str->bigdec })))
  {
   :liikennevuositilasto  [s/Liikennekuukausi]
   :liikenneviikkotilasto [s/Liikennepaiva]
   :lipputulo             [s/Lipputulo]
   :liikennointikorvaus   [s/Liikennointikorvaus]
   :lippuhinta            [s/Lippuhinta]
   :alue                  (to-optional-key s/Alue :kustannus)
   }))

(defn dissoc-id [data]
  (map (c/partial-first-arg dissoc :tunnuslukutyyppi :vuosi :organisaatioid :sopimustyyppitunnus) data))

(defn pivot-data [pivot data]
  (cond
    (nil? pivot) data
    (= pivot :all) (m/flat->tree (apply merge data)  #"_")
    :else (map (partial apply merge) (vals (group-by pivot data)))))

(defn map->bulletpoints [map]
  (str/join "\n" (for [[key value] map] (str "- " key " - " value))))

(defn validation-error? [v]
  (instance? ValidationError v))

(defn invalid-fields-bulletpoints [error]
  (reduce
    (fn [acc v]
      (cond
        (map? v) (str acc (invalid-fields-bulletpoints v))
        (map/mapentry? v)
          (let [[key value] v]
            (if (validation-error? value)
              (str acc "\n-- "  (name key) ": '" (.value value) "'")
              (str acc "\n-- "  (name key) ": " value )))
        :else acc))
    ""
    (if (map? error) (m/tree->flat error "-") error)))

(defn import-csv [data]
  (let [columns (count (first data))]
    (when-not (every? (coll/eq count columns) data)
      (ss/throw+ {:http-response r/bad-request} "Tunnuslukutaulukossa kaikilla riveillä ei ole sama määrä sarakkeita")))

  (let [headers (map str/lower-case (first data))
        organisaatiot (org/organisaatiot)
        tunnusluvut (->> (rest data)
                         (map (c/partial-first-arg update 0 parse-int))
                         (filter (comp c/not-nil? first))
                         (filter (comp strx/not-blank? second))
                         (mapcat (partial unpivot-organizations organisaatiot headers))
                         (map (partial m/zip-object [:vuosi :tunnusluku :organisaatioid :value]))
                         (map parse-tunnusluku)
                         (filter (comp c/not-nil? :tunnuslukutyyppi))
                         (group-by (juxt :tunnuslukutyyppi :vuosi :organisaatioid :sopimustyyppitunnus)))]

    (let [result
      (for [[[tunnuslukutyyppi vuosi organisaatioid sopimustyyppitunnus] data] tunnusluvut]
        (let [tunnusluku-name (str vuosi "-" (name tunnuslukutyyppi) "-"
                                   (-> (org/find-organisaatio organisaatioid organisaatiot) :nimi str/lower-case (str/replace " " "-"))
                                   (strx/blank-if-nil "-" sopimustyyppitunnus))
              save (tunnusluku-save tunnuslukutyyppi)
              coercer (tunnusluku-coercer tunnuslukutyyppi)
              pivoted-data (pivot-data (tunnusluku-pivot tunnuslukutyyppi) (dissoc-id data))
              coerced-data (coercer pivoted-data)]
          (if (sc-util/error? coerced-data)
            {:tunnusluku tunnusluku-name :status "syntax-error"
             :error (sc-util/error-val coerced-data) :data pivoted-data}
            (ss/try+
              (if (nil? sopimustyyppitunnus)
                (save vuosi organisaatioid coerced-data)
                (save vuosi organisaatioid sopimustyyppitunnus coerced-data))
              {:tunnuslukutyyppi tunnuslukutyyppi :vuosi vuosi :tunnusluku tunnusluku-name :status "success"}
              (catch Object t
                {:tunnusluku tunnusluku-name :status "db-error"
                 :error &throw-context :data coerced-data})))))]

      ;; tulosten raportointi
      (str "Tunnuslukuja ladattiin onnistuneesti: \n"
           (map->bulletpoints (into (sorted-map)
                                    (map/map-values count (group-by #(str (:vuosi %) "-" (name (:tunnuslukutyyppi %)))
                                                                    (filter (coll/eq :status "success") result)))))
           "\n\nLatausvirheet: \n"
           (str/join "\n" (map #(str "- " (:tunnusluku %) " vialliset kentät: " (invalid-fields-bulletpoints (:error %)))
                               (filter (coll/eq :status "syntax-error") result)))
           "\n\nTietokantavirheet: \n"
           (str/join "\n" (map #(str "- " (:tunnusluku %) " virhe: " (:error %))
                               (filter (coll/eq :status "db-error") result)))))))

; *** Tunnuslukujen täyttöaste ***

(defn tayttoaste [vuosi organisaatioid]
  (let [organisaatio (org/get-organisaatio! (bigdec organisaatioid))
        pisteet (-> (select-tayttoaste-pisteet (c/bindings->map vuosi organisaatioid)) first :pisteet)
        max-pisteet (if (= (:lajitunnus organisaatio) "KS3") 328 325)]
    (with-precision 3 :rounding HALF_UP (/ pisteet max-pisteet))))

(defn tayttoasteet [vuodet]
  (for [vuosi vuodet
        organisaatio (org/organisaatiot)]
    [vuosi (:nimi organisaatio) (* (tayttoaste vuosi (:id organisaatio)) 100)]))

; *** Tunnuslukujen export ***

(defn resultset->out-as-csv [output resultset]
  (let [header (first resultset)
        ^Writer w (io/writer output)]

    ; UTF-8 Byte-order marker will clue Excel 2007+ in to the fact that you're using UTF-8
    ; see http://stackoverflow.com/questions/6002256/is-it-possible-to-force-excel-recognize-utf-8-csv-files-automatically
    (.write w "\uFEFF")

    (.write w (csv/write-csv [(map name header)] :delimiter ";"))
    (.flush w)
    (doseq [row (rest resultset)]
      (.write w (csv/write-csv [(map str (update row 9 common/format-number))] :delimiter ";"))
      (.flush w))))

(defn export-tunnusluvut-csv [output]
  (select-all-tunnusluvut {} {:connection db :as-arrays? true
                              :result-set-fn (partial resultset->out-as-csv output)}))