(ns juku.service.seuranta
  (:require [juku.db.yesql-patch :as sql]
            [juku.db.database :refer [db with-transaction]]
            [juku.db.coerce :as coerce]
            [juku.db.sql :as dml]
            [common.collection :as coll]
            [clj-pdf.core :as pdf]
            [juku.service.pdf :as juku-pdf]
            [juku.service.hakemus-core :as hc]
            [juku.schema.seuranta :as s]
            [ring.util.http-response :as r]
            [slingshot.slingshot :as ss]
            [common.collection :as coll]
            [common.map :as m]))

; *** Seurantatietoihin liittyvät kyselyt ***
(sql/defqueries "seuranta.sql")

; *** Seuranta-skeemaan liittyvät konversiot tietokannan tietotyypeistä ***
(def coerce-liikennesuorite (coerce/coercer s/Liikennesuorite))
(def coerce-lippusuorite (coerce/coercer s/Lippusuorite))

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

(def lippusuorite-constraint-errors
  {:lippusuorite_pk {:http-response r/bad-request
                     :message "Lippusuorite {lipputyyppitunnus}-{numero} on jo olemassa hakemuksella (id = {hakemusid})."}
   :lippusuorite_hakemus_fk {:http-response r/not-found
                             :message "Lippusuoritteen {lipputyyppitunnus}-{numero} hakemusta (id = {hakemusid}) ei ole olemassa."}
   :lippusuorite_lputyyppi_fk {:http-response r/not-found
                               :message "Lipputyyppiä {lipputyyppitunnus} ei ole olemassa."}})

; *** Liikennesuoritteiden toiminnot ***
(defn- insert-liikennesuorite [suorite]
  (:id (dml/insert db "liikennesuorite" suorite liikennesuorite-constraint-errors suorite)))

(defn save-liikennesuoritteet! [hakemusid suoritteet]
  (hc/assert-edit-hakemus-content-allowed*! (hc/get-hakemus hakemusid))

  (with-transaction
    (delete-hakemus-liikennesuorite! {:hakemusid hakemusid})
    (doseq [suorite suoritteet]
      (insert-liikennesuorite (assoc suorite :hakemusid hakemusid))))
  nil)

(defn find-hakemus-liikennesuoritteet [hakemusid]
  (hc/assert-view-hakemus-content-allowed*! (hc/get-hakemus hakemusid))

  (map coerce-liikennesuorite (select-hakemus-liikennesuorite {:hakemusid hakemusid})))

(defn find-suoritetyypit [] (select-suoritetyypit))

; *** Lippusuoritteiden toiminnot ***
(defn- insert-lippusuorite [suorite]
  (:id (dml/insert db "lippusuorite" suorite lippusuorite-constraint-errors suorite)))

(defn save-lippusuoritteet! [hakemusid suoritteet]
  (hc/assert-edit-hakemus-content-allowed*! (hc/get-hakemus hakemusid))

  (with-transaction
    (delete-hakemus-lippusuorite! {:hakemusid hakemusid})
    (doseq [suorite suoritteet]
      (insert-lippusuorite (assoc suorite :hakemusid hakemusid))))
  nil)

(defn find-hakemus-lippusuoritteet [hakemusid]
  (hc/assert-view-hakemus-content-allowed*! (hc/get-hakemus hakemusid))

  (map coerce-lippusuorite (select-hakemus-lippusuorite {:hakemusid hakemusid})))

(defn find-lipputyypit [] (select-lipputyypit))

(def pdf-metadata
  {:title  "Joukkoliikenteen valtionavun maksatushakemus"
   :subject "Seurantatiedot"
   :author "Liikennevirasto"
   :header "Joukkoliikenteen valtionavun maksatushakemuksen seurantatiedot"
   :orientation   :landscape
   :size :a4
   :font  {:size 11}})

(defn liikennesuorite-table [suoritteet suoritetyypit]
  (if (empty? suoritteet)
    [:paragraph "Ei tietoja"]
    (let [header [{:backdrop-color [200 200 200]}
                  "Suoritetyyppi" "Suoritteen nimi" "Linja-autot" "Taksit"
                  "Ajokilometrit" "Matkustajat" "Lipputulo (€)" "Netto (€)" "Brutto (€)"]
          suorite->str (juxt (comp suoritetyypit :suoritetyyppitunnus)
                             :nimi
                             (comp juku-pdf/format-number :linjaautot)
                             (comp juku-pdf/format-number :taksit)
                             (comp juku-pdf/format-number :ajokilometrit)
                             (comp juku-pdf/format-number :matkustajamaara)
                             (comp juku-pdf/format-number :lipputulo)
                             (comp juku-pdf/format-number :nettohinta)
                             #(juku-pdf/format-number (+ (:nettohinta %) (:lipputulo %))))]

      (->> (map suorite->str suoritteet)
          (cons {:header header})
          (cons :table)
          vec))))

(defn lippusuorite-table [kaupunki? suoritteet lipputyypit]
  (if (empty? suoritteet)
    [:paragraph "Ei tietoja"]
    (let [header [{:backdrop-color [200 200 200]}
                  (if kaupunki? "Kaupunkilippu" "Seutulippu")
                  "Myynti (kpl)"
                  "Matkat (kpl)"
                  "Asiakashinta"
                  "Keskipituus (km)"
                  "Lipputulot (€)"
                  "Julkinen rahoitus (€)"]
          suorite->str (juxt (if kaupunki? (comp lipputyypit :lipputyyppitunnus)
                                           :seutulippualue)
                             (comp juku-pdf/format-number :myynti)
                             (comp juku-pdf/format-number :matkat)
                             (comp juku-pdf/format-number :asiakashinta)
                             (comp juku-pdf/format-number :keskipituus)
                             (comp juku-pdf/format-number :lipputulo)
                             (comp juku-pdf/format-number :julkinenrahoitus))]

      (->> (map suorite->str suoritteet)
           (cons {:header header})
           (cons :table)
           vec))))

(defn pdf-template [psa-table pal-table kaupunki-table seutu-table]
  [pdf-metadata
   [:spacer ]
   [:heading "Paikallisliikenne tai muu PSA:n mukainen liikenne"]
   psa-table
   [:spacer ]
   [:heading "Palveluliikenne"]
   pal-table
   [:spacer ]
   [:heading "Kaupunkiliput"]
   kaupunki-table
   [:spacer ]
   [:heading "Seutuliput"]
   seutu-table])

(defn seurantatieto-pdf [hakemusid output]
  (let [suoritetyypit (m/map-values (comp :nimi first) (group-by :tunnus (find-suoritetyypit)))
        liikennesuoritteet (find-hakemus-liikennesuoritteet hakemusid)
        psa-suoriteet (filter (coll/eq :liikennetyyppitunnus "PSA") liikennesuoritteet)
        pal-suoriteet (filter (coll/eq :liikennetyyppitunnus "PAL") liikennesuoritteet)
        lipputyypit (m/map-values (comp :nimi first) (group-by :tunnus (find-lipputyypit)))
        lippusuoritteet (find-hakemus-lippusuoritteet hakemusid)
        kaupunki-suoritteet (filter (coll/predicate not= :lipputyyppitunnus "SE") lippusuoritteet)
        seutu-suoritteet (filter (coll/eq :lipputyyppitunnus "SE") lippusuoritteet)]
    (pdf/pdf (pdf-template
               (liikennesuorite-table psa-suoriteet suoritetyypit)
               (liikennesuorite-table pal-suoriteet suoritetyypit)
               (lippusuorite-table true kaupunki-suoritteet lipputyypit)
               (lippusuorite-table false seutu-suoritteet lipputyypit)) output)))