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

(defn get-asiakirjamalli! [vuosi asiakirjalajitunnus hakemustyyppitunnus organisaatiolajitunnus]
  (c/assert-not-nil!
    (find-asiakirjamalli vuosi asiakirjalajitunnus hakemustyyppitunnus organisaatiolajitunnus)
    {:message (str "Asiakirjamallia ei ole määritetty hakemustyypille: " hakemustyyppitunnus
                   ", organisaatiolajille: " organisaatiolajitunnus " ja vuodelle: " vuosi)}))

(defn edit-asiakirjamalli! [id asiakirjamalli]
  (update-asiakirjamalli! (assoc asiakirjamalli :id id)))

(defn add-asiakirjamalli! [asiakirjamalli]
  (insert-asiakirjamalli! asiakirjamalli))

(defn delete-asiakirjamalli! [id]
  (update-asiakirjamalli-mark-deleted! {:id id}))

(def example-avustuskohteet
  {:avustuskohteet "|3|2|\n|-|-|\n| **PSA:n mukaisen liikenteen hankinta (alv 0%)** | | \n| Paikallisliikenne | 200 000 € |\n| Integroitupalvelulinja | 200 000 € |\n| Muu PSA:n mukaisen liikenteen järjestäminen | 200 000 € |\n\n|3|2|\n|-|-|\n| **Hintavelvoitteiden korvaaminen (alv 10%)** | | \n| Seutulippu | 220 000 € |\n| Kaupunkilippu tai kuntalippu | 220 000 € |\n| Liityntälippu | 220 000 € |\n| Työmatkalippu | 220 000 € |\n\n|3|2|\n|-|-|\n| **Liikenteen suunnittelu ja kehittämishankkeet (alv 0%)** | | \n| Informaatio ja maksujärjestelmien kehittäminen | 200 000 € |\n| Matkapalvelukeskuksen suunnittelu ja kehittäminen | 200 000 € |\n| Matkakeskuksen suunnittelu ja kehittäminen | 200 000 € |\n| Raitiotien suunnittelu | 200 000 € |\n| Muu hanke | 200 000 € |",
   :avustuskohteet-summary "PSA:n mukaisen liikenteen hankinta 600 000 € (alv 0%)\nHintavelvoitteiden korvaaminen 880 000 € (alv 10%) sisältäen arvonlisäveron osuuden 80 000 €.\nLiikenteen suunnittelu ja kehittämishankkeet 1 000 000 € (alv 0%)",
   :haettuavustus "2 480 000",
   :omarahoitus "2 980 000",
   :omarahoitus-all "2 980 000",
   :omarahoitus-all-selite ""})

(def example-common-hakemus
  {:vireillepvm "18.12.2019",
   :organisaatio-nimi "Helsingin seudun liikenne",
   :organisaatiolaji-pl-gen "suurten kaupunkiseutujen",
   :lahettaja "<hakijan nimi, joka on lähettänyt hakemuksen>",
   :vuosi 2020,
   :liitteet "esimerkki-liite1.pdf\nesimerkki-liite2.pdf"})

(def example-ely-hakemus
  {:organisaatio-nimi "Uusimaa ELY",
   :organisaatiolaji-pl-gen "ELY-keskusten",
   :haettuavustus "3 502 230",
   :ostot "100 000",
   :kaupunkilipputuki "100 000",
   :seutulipputuki "100 000",
   :kehittaminen "100 000",
   :kehityshankkeet "|Kehityshanke 1|10 000 €|\n|Kehityshanke 2|66 666 €|",
   :maararahatarpeet "|3|2|\n|-|-|\n|**Bruttosopimus**||\n|Sidotut kustannukset|100 000 €|\n|Uudet sopimukset|200 000 €|\n|Kauden tulot|100 000 €|\n\n|3|2|\n|-|-|\n|**Hintavelvoitteiden korvaaminen**||\n|Sidotut kustannukset|100 000 €|\n|Uudet sopimukset|200 000 €|\n\n|3|2|\n|-|-|\n|**Käyttöoikeussopimuskorvaukset (alueellinen)**||\n|Sidotut kustannukset|100 000 €|\n|Uudet sopimukset|200 000 €|\n\n|3|2|\n|-|-|\n|**Käyttöoikeussopimuskorvaukset (reitti)**||\n|Sidotut kustannukset|100 000 €|\n|Uudet sopimukset|200 000 €|\n\n|3|2|\n|-|-|\n|**Muu PSA:n mukainen liikenne**||\n|Sidotut kustannukset|100 000 €|\n|Uudet sopimukset|200 000 €|"})

(def example-paatos
  {:paattaja "<päätöksen hyväksyneen käyttäjän nimi>",
   :esittelija "<hakemuksen tarkastaneen käyttäjän nimi>",
   :paatosspvm "1.1.2020",
   :myonnettyavustus "2 480 000",
   :selite "",
   :alv-selite "\n\n\tAvustukseen sisältyy arvonlisävero hintavelvoitteen korvaamisen osalta.",

   :organisaatio-nimi "Helsingin seudun liikenne",
   :organisaatiolaji-pl-gen "suurten kaupunkiseutujen",
   :lahetyspvm "18.12.2019",
   :vuosi 2020,
   :mh1-hakuaika-loppupvm "31.8.2020",
   :mh2-hakuaika-loppupvm "31.1.2021"})

(def example-mh-paatos
  {:osuusavustuksesta "50",
   :ah0-myonnettyavustus "2 480 000",
   :ah0-paatospvm "1.1.2020",
   :momentti "31.30.63.09"})

(def example-mh2-paatos
  {:mh1-paatospvm "1.1.2020",
   :mh1-myonnettyavustus "1 240 000"})

(def example-ely-paatos
  {:organisaatio-nimi "Uusimaa ELY",
   :organisaatiolaji-pl-gen "ELY-keskusten",
   :maararahakiintiot "|Uusimaa ELY|1 000 000 €|\n|Varsinais-Suomi ELY|1 000 000 €|\n|Kaakkois-Suomi ELY|1 000 000 €|\n|Pirkanmaa ELY|1 000 000 €|\n|Pohjois-Savo ELY|1 000 000 €|\n|Keski-Suomi ELY|1 000 000 €|\n|Etelä-Pohjanmaa ELY|1 000 000 €|\n|Pohjois-Pohjanmaa ELY|1 000 000 €|\n|Lappi ELY|1 000 000 €|",
   :maararaha "4 000 000",
   :jakamatta "1 000 000",
   :viimevuosi 2019,
   :myonnettyavustus "3 000 000"})

(def preview-template-values
  {:h
   {:ah0 (merge example-common-hakemus example-avustuskohteet)
    :mh1 (merge example-common-hakemus example-avustuskohteet)
    :mh2 (merge example-common-hakemus example-avustuskohteet)
    :ely (merge example-common-hakemus example-ely-hakemus)}
   :p
   {:ah0 (merge example-paatos example-avustuskohteet)
    :mh1 (merge example-mh-paatos example-paatos example-avustuskohteet)
    :mh2 (merge example-mh2-paatos example-mh-paatos example-paatos example-avustuskohteet)
    :ely (merge example-paatos example-ely-paatos)}})

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
  (add-embedded-template 2016 "H" "MH1" nil)
  (add-embedded-template 2016 "H" "MH2" nil)
  (add-embedded-template 2016 "H" "ELY" nil)
  (add-embedded-template 2019 "P" "AH0" "KS1")
  (add-embedded-template 2019 "P" "AH0" "KS2")
  (add-embedded-template 2018 "P" "MH1" nil)
  (add-embedded-template 2018 "P" "MH2" nil)
  (add-embedded-template 2016 "P" "ELY" nil))