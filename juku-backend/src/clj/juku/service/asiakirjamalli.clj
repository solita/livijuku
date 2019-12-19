(ns juku.service.asiakirjamalli
  (:require [juku.db.yesql-patch :as sql]
            [clojure.java.io :as io]
            [common.core :as c]
            [common.string :as xstr]
            [ring.util.http-response :as r]
            [clojure.string :as str]
            [schema.coerce :as scoerce]
            [juku.db.coerce :as coerce]
            [juku.schema.asiakirjamalli :as s]
            [juku.service.pdf2 :as pdf]
            [juku.service.hakemus-core :as h]
            [clj-time.core :as time]))

(def constraint-errors
  {:asiakirjamalli_u {:http-response r/conflict :message "Asiakirjamalli on jo olemassa"}
   :asiakirjamalli_hatyyppi_fk {:http-response r/not-found :message "Hakemustyyppiä: {hakemustyyppitunnus} ei ole olemasssa."}})


(sql/defqueries "asiakirjamalli.sql" {:constraint-errors constraint-errors
                                      :dissoc-error-params [:sisalto]})

(def coerce-asiakirja (scoerce/coercer s/Asiakirjamalli coerce/db-coercion-matcher))
(def coerce-asiakirja+ (scoerce/coercer s/Asiakirjamalli+sisalto coerce/db-coercion-matcher))

(defn find-all [] (map coerce-asiakirja (select-all-asiakirjamallit)))

(defn find-by-id [id] (first (map coerce-asiakirja+ (select-asiakirjamalli-by-id {:id id}))))

(defn find-asiakirjamalli [vuosi asiakirjalajitunnus hakemustyyppitunnus organisaatiolajitunnus]
  (first (map (comp coerce/clob->reader :sisalto)
           (select-asiakirjamalli-sisalto
             (c/bindings->map vuosi asiakirjalajitunnus
                              hakemustyyppitunnus organisaatiolajitunnus)))))

(defn edit-asiakirjamalli! [id asiakirjamalli]
  (update-asiakirjamalli! (assoc asiakirjamalli :id id)))

(defn add-asiakirjamalli! [asiakirjamalli]
  (insert-asiakirjamalli! asiakirjamalli))

(defn delete-asiakirjamalli! [id]
  (update-asiakirjamalli-mark-deleted! {:id id}))

(def preview-template-values
  {:h
   {:ah0 {:avustuskohteet-summary "\tPSA:n mukaisen liikenteen hankinta 600 000 e (alv 0%)\n\n\tHintavelvoitteiden korvaaminen 880 000 e (alv 10%) sisältäen arvonlisäveron osuuden 80 000 e.\n\n\tLiikenteen suunnittelu ja kehittämishankkeet 1 000 000 e (alv 0%)",
          :vireillepvm "18.12.2019",
          :organisaatio-nimi "Helsingin seudun liikenne",
          :omarahoitus "2 980 000",
          :omarahoitus-all-selite "",
          :omarahoitus-all "2 980 000",
          :organisaatiolaji-pl-gen "suurten kaupunkiseutujen",
          :haettuavustus "2 480 000",
          :lahettaja "<hakijan nimi, joka on lähettänyt hakemuksen>",
          :vuosi 2020,
          :avustuskohteet "|3|2|\n|-|-|\n| **PSA:n mukaisen liikenteen hankinta (alv 0%)** | | \n| Paikallisliikenne | 200 000 € |\n| Integroitupalvelulinja | 200 000 € |\n| Muu PSA:n mukaisen liikenteen järjestäminen | 200 000 € |\n\n|3|2|\n|-|-|\n| **Hintavelvoitteiden korvaaminen (alv 10%)** | | \n| Seutulippu | 220 000 € |\n| Kaupunkilippu tai kuntalippu | 220 000 € |\n| Liityntälippu | 220 000 € |\n| Työmatkalippu | 220 000 € |\n\n|3|2|\n|-|-|\n| **Liikenteen suunnittelu ja kehittämishankkeet (alv 0%)** | | \n| Informaatio ja maksujärjestelmien kehittäminen | 200 000 € |\n| Matkapalvelukeskuksen suunnittelu ja kehittäminen | 200 000 € |\n| Matkakeskuksen suunnittelu ja kehittäminen | 200 000 € |\n| Raitiotien suunnittelu | 200 000 € |\n| Muu hanke | 200 000 € |",
          :liitteet "whyfp90.pdf"}}})

(defn preview-asiakirjamalli [asiakirjamalli]
  (let [template (:sisalto asiakirjamalli)
        template-values
        (get-in preview-template-values
            [(-> asiakirjamalli :asiakirjalajitunnus str/lower-case keyword)
             (or (some-> asiakirjamalli :hakemustyyppitunnus str/lower-case keyword) :ah0)])]
    (pdf/pdf->inputstream
      "Esimerkkihakemus" (h/format-date (time/today)) "1234567890ABCD"
      "Esikatselu"
      (xstr/interpolate template template-values))))

(defn preview [id]
  (preview-asiakirjamalli (find-by-id id)))

(defn- find-embedded-template [vuosi asiakirjalajitunnus hakemustyyppitunnus organisaatiolajitunnus]
  (slurp (io/reader (io/resource
                      (str "pdf-sisalto/templates/"
                           (case (str/lower-case asiakirjalajitunnus) "h" "hakemus" "p" "paatos")
                           (or (some->> hakemustyyppitunnus str/lower-case (str "-")) "")
                           (or (some->> organisaatiolajitunnus str/lower-case (str "-")) "")
                           "-" vuosi ".txt")))))

(defn- add-embedded-template [voimaantulovuosi asiakirjalajitunnus hakemustyyppitunnus organisaatiolajitunnus]
  (add-asiakirjamalli!
    (assoc (c/bindings->map voimaantulovuosi asiakirjalajitunnus
                            hakemustyyppitunnus organisaatiolajitunnus)
      :sisalto (find-embedded-template voimaantulovuosi asiakirjalajitunnus
                                       hakemustyyppitunnus organisaatiolajitunnus))))

(when (empty? (find-all))
  (add-embedded-template 2016 "H" "AH0" nil)
  (add-embedded-template 2019 "P" "AH0" "KS1"))