(ns juku.service.paatos-pdf-test
  (:require [midje.sweet :refer :all]
            [clojure.string :as str]
            [common.collection :as coll]
            [common.map :as m]
            [clj-time.core :as time]
            [clj-time.format :as timef]
            [juku.db.coerce :as dbc]
            [juku.service.pdf-mock :as pdf]
            [juku.service.paatos :as p]
            [juku.service.hakemus-core :as hc]
            [juku.service.hakemus :as h]
            [juku.service.hakemuskausi :as hk]
            [juku.service.liitteet :as l]
            [juku.service.avustuskohde :as ak]
            [juku.service.asiahallinta-mock :as asha]
            [juku.service.test :as test]
            [juku.headers :as headers]
            [common.core :as c]
            [common.string :as strx])
  (:import (java.io InputStream)))

(defn find-by-id [id] (fn [m] (= (:id m) id)))

(def hakemuskausi (test/next-hakemuskausi!))
(def vuosi (:vuosi hakemuskausi))
(hk/update-hakemuskausi-set-diaarinumero! {:vuosi vuosi :diaarinumero (str "dnro:" vuosi)})

(def hsl-ah0-hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid 1M})

(defn add-hakemus! [kausi tyyppi] (hc/add-hakemus! {:vuosi (:vuosi kausi) :hakemustyyppitunnus tyyppi :organisaatioid 1M}))

(defmacro test-ctx [& body]
  `(test/with-user "juku_hakija" ["juku_hakija" "juku_kasittelija"]
     (asha/with-asha
       (pdf/with-mock-pdf ~@body))))

(defn assert-hsl-avustushakemuspaatos-teksti []
  (fact "HSL avustushakemuspäätöksen sisällön tarkastaminen"
    (let [teksti (:teksti pdf/*mock-pdf*)]
      teksti => (partial strx/substring? "Hakija: Helsingin seudun liikenne")
      teksti => (partial strx/substring? (str "Hakija hakee vuodelle " vuosi " suurten kaupunkiseutujen joukkoliikenteen valtionavustusta 0 euroa."))
      teksti => (partial strx/substring? (str "Hakija osoittaa omaa rahoitusta näihin kohteisiin yhteensä 0 euroa."))

      teksti => (partial strx/substring? (str "seurantatiedot ajalta 1.1. - 30.6." vuosi " päivämäärään 31.8." vuosi " mennessä."))
      teksti => (partial strx/substring? (str "seurantatiedot ajalta 1.7. - 31.12." vuosi " päivämäärään 31.1." (+ vuosi 1) " mennessä.")))))

(defn assert-hsl-maksatushakemuspaatos-teksti [vuosi kausi summa]
  (fact "HSL maksatushakemuspäätöksen sisällön tarkastaminen"
    (let [teksti (:teksti pdf/*mock-pdf*)]
      teksti => (partial strx/substring? "Hakija: Helsingin seudun liikenne")

      teksti => (partial strx/substring? (str "Hakija on toimittanut Liikennevirastolle <lähetyspäivämäärä> "
                                              "joukkoliikenteen maksatushakemuksen ajalta " kausi vuosi
                                              " JUKU-järjestelmään. Hakija hakee valtionavustusta maksuun yhteensä " summa " euroa."))

      teksti => (partial strx/substring? (str "Hakija on käyttänyt omaa rahoitusta näihin kohteisiin yhteensä " summa " euroa.")))))

(defn assert-hsl-maksatushakemuspaatos1-teksti [vuosi ah0-paatospvm ah0-myonnettyavustus osuus-avustuksesta haettuavustus]
  (fact "HSL 1. maksatushakemuspäätöksen sisällön tarkastaminen"
    (let [teksti (:teksti pdf/*mock-pdf*)]
      teksti => (partial strx/substring? (str "Liikennevirasto on myöntänyt hakijalle " ah0-paatospvm
                                              " tehdyllä päätöksellä joukkoliikenteen valtionavustusta vuodelle " vuosi
                                              " arvonlisäveroineen yhteensä enintään " ah0-myonnettyavustus " euroa."))

      teksti => (partial strx/substring? (str "Hakija hakee ajalta 1.1. - 30.6." vuosi " muodostuneita kustannuksia maksuun "
                                              "yhteensä " haettuavustus " euroa, joka on " osuus-avustuksesta " % vuodelle " vuosi " myönnetystä "
                                              "valtionavustuksesta. "))

      teksti => (partial strx/substring? (str "Avustus maksetaan valtion vuoden " vuosi " talousarvion momentin 31.30.63.09 määrärahasta")))))

(defn assert-hsl-maksatushakemuspaatos2-teksti [mh1-paatospvm mh1-myonnettyavustus]
  (fact "HSL 1. maksatushakemuspäätöksen sisällön tarkastaminen"
    (let [teksti (:teksti pdf/*mock-pdf*)]
      teksti => (partial strx/substring? (str "Liikennevirasto on " mh1-paatospvm " tehdyllä päätöksellä maksanut "
                                              mh1-myonnettyavustus " euroa hakijalle.")))))


(fact "Avustushakemuksen päätöksen esikatselu"
  (test-ctx
    (let [id (hc/add-hakemus! hsl-ah0-hakemus)]

      (p/find-paatos-pdf id) => (partial instance? InputStream)

      (assert-hsl-avustushakemuspaatos-teksti)
      (pdf/assert-otsikko "Valtionavustuspäätös" "<päätöspäivämäärä>" nil)

      (:footer pdf/*mock-pdf*) => (partial strx/substring? "esikatselu"))))

(fact "1. maksatushakemuksen päätöksen esikatselu"
  (test-ctx
    (let [kausi (test/next-avattu-empty-hakemuskausi!)
          id (add-hakemus! kausi "MH1")]

      (p/find-paatos-pdf id) => (partial instance? InputStream)

      (assert-hsl-maksatushakemuspaatos-teksti (:vuosi kausi) "1.1. - 30.6." 0)
      (assert-hsl-maksatushakemuspaatos1-teksti (:vuosi kausi) "<avustuksen myöntämispvm>" "<myönnetty avustus>" "<osuus avustuksesta>" 0)

      (pdf/assert-otsikko "Valtionavustuspäätös" "<päätöspäivämäärä>" nil)

      (:footer pdf/*mock-pdf*) => (partial strx/substring? "esikatselu"))))

(fact "2. maksatushakemuksen päätöksen esikatselu"
  (test-ctx
    (let [kausi (test/next-avattu-empty-hakemuskausi!)
          id (add-hakemus! kausi "MH2")]

      (p/find-paatos-pdf id) => (partial instance? InputStream)

      (assert-hsl-maksatushakemuspaatos-teksti (:vuosi kausi) "1.7. - 31.12." 0)
      (assert-hsl-maksatushakemuspaatos2-teksti "<maksatuspäätös pvm>" "<maksettu avustus>")
      (pdf/assert-otsikko "Valtionavustuspäätös" "<päätöspäivämäärä>" nil)

      (:footer pdf/*mock-pdf*) => (partial strx/substring? "esikatselu"))))

(fact "Voimassaolevan päätöksen hakeminen"
  (test-ctx
    (let [id (hc/add-hakemus! hsl-ah0-hakemus)
          paatos {:hakemusid id, :myonnettyavustus 1M :selite "FooBar"}]
      (p/save-paatos! paatos)
      (h/laheta-hakemus! id)
      (h/tarkasta-hakemus! id)
      (p/hyvaksy-paatos! id)

      (p/find-paatos-pdf id) => (partial instance? InputStream)
      (pdf/assert-otsikko "Valtionavustuspäätös" "testing")
      (assert-hsl-avustushakemuspaatos-teksti)
      (:footer pdf/*mock-pdf*) => "Liikennevirasto")))

(fact "Maksatushakemuksen päätöksen esikatselu avustushakemuksen päätös on tehty"
  (test-ctx
    (let [kausi (test/next-avattu-empty-hakemuskausi!)
          ah0 (add-hakemus! kausi "AH0")
          id (add-hakemus! kausi "MH1")
          paatos {:hakemusid ah0, :myonnettyavustus 1M :selite "FooBar"}]

      (p/save-paatos! paatos)
      (h/laheta-hakemus! ah0)
      (h/tarkasta-hakemus! ah0)
      (p/hyvaksy-paatos! ah0)

      (ak/add-avustuskohde! {:hakemusid id
                             :avustuskohdeluokkatunnus "PSA"
                             :avustuskohdelajitunnus "1"
                             :haettavaavustus 1,
                             :omarahoitus 1})

      (p/find-paatos-pdf id)

      (assert-hsl-maksatushakemuspaatos-teksti (:vuosi kausi) "1.1. - 30.6." 1)
      (assert-hsl-maksatushakemuspaatos1-teksti (:vuosi kausi) pdf/today 1 100 1)

      (pdf/assert-otsikko "Valtionavustuspäätös" "<päätöspäivämäärä>" "testing")

      (:footer pdf/*mock-pdf*) => (partial strx/substring? "esikatselu"))))


(fact "Päätöksen esittelijä ja päättäjä"
  (test/with-user "juku_kasittelija" ["juku_kasittelija"]
    (asha/with-asha
      (pdf/with-mock-pdf
        (let [id (hc/add-hakemus! hsl-ah0-hakemus)
              paatos {:hakemusid id, :myonnettyavustus 1M :selite "FooBar" :paattajanimi "Pentti Päättäjä"}]

          (p/save-paatos! paatos)
          (test/with-hakija (h/laheta-hakemus! id))
          (h/tarkasta-hakemus! id)
          (p/hyvaksy-paatos! id)

          (p/find-paatos-pdf id) => (partial instance? InputStream)
          (pdf/assert-otsikko "Valtionavustuspäätös" "testing")
          (assert-hsl-avustushakemuspaatos-teksti)

          (let [teksti (:teksti pdf/*mock-pdf*)]
            teksti => (partial strx/substring? "Päättäjä\tPentti Päättäjä")
            teksti => (partial strx/substring? "Esittelijä\tKatri Käsittelijä"))

          (:footer pdf/*mock-pdf*) => "Liikennevirasto")))))

(fact "Maksatushakemuksessa (mh1) osuusavustuksesta on päättymätön murtoluku"
  (test-ctx
    (let [kausi (test/next-avattu-empty-hakemuskausi!)
          ah0 (add-hakemus! kausi "AH0")
          mh1 (add-hakemus! kausi "MH1")
          paatos {:hakemusid ah0, :myonnettyavustus 33000M :selite "FooBar" :paattajanimi "Pentti Päättäjä"}
          ak {:hakemusid     mh1
              :avustuskohdeluokkatunnus "PSA"
              :avustuskohdelajitunnus "1"
              :haettavaavustus 16000
              :omarahoitus 16000}]

      (h/laheta-hakemus! ah0)
      (h/tarkasta-hakemus! ah0)
      (p/save-paatos! paatos)
      (p/hyvaksy-paatos! ah0)

      (ak/add-avustuskohde! ak)

      (p/find-paatos-pdf mh1) => (partial instance? InputStream)

      ;; Huom! luvuissa oleva välilyönti on nbsp (00A0) ei siis normaali välilyönti (0020)
      (assert-hsl-maksatushakemuspaatos-teksti (:vuosi kausi) "1.1. - 30.6." "16 000")
      (assert-hsl-maksatushakemuspaatos1-teksti (:vuosi kausi) pdf/today "33 000" 48 "16 000")
      (pdf/assert-otsikko "Valtionavustuspäätös" "<päätöspäivämäärä>" "testing"))))

(fact "Maksatushakemuksessa (mh1) osuusavustuksesta on 50%"
  (test-ctx
    (let [kausi (test/next-avattu-empty-hakemuskausi!)
          ah0 (add-hakemus! kausi "AH0")
          mh1 (add-hakemus! kausi "MH1")
          paatos {:hakemusid ah0, :myonnettyavustus 2 :selite "FooBar" :paattajanimi "Pentti Päättäjä"}
          ak {:hakemusid     mh1
              :avustuskohdeluokkatunnus "PSA"
              :avustuskohdelajitunnus "1"
              :haettavaavustus 1
              :omarahoitus 1}]

      (h/laheta-hakemus! ah0)
      (h/tarkasta-hakemus! ah0)
      (p/save-paatos! paatos)
      (p/hyvaksy-paatos! ah0)

      (ak/add-avustuskohde! ak)

      (p/find-paatos-pdf mh1) => (partial instance? InputStream)
      (assert-hsl-maksatushakemuspaatos-teksti (:vuosi kausi) "1.1. - 30.6." 1)
      (assert-hsl-maksatushakemuspaatos1-teksti (:vuosi kausi) pdf/today 2 50 1)
      (pdf/assert-otsikko "Valtionavustuspäätös" "<päätöspäivämäärä>" "testing"))))

(fact "Maksatushakemus (mh1) - avustusta myönnetty 0€"
  (test-ctx
    (let [kausi (test/next-avattu-empty-hakemuskausi!)
          ah0 (add-hakemus! kausi "AH0")
          mh1 (add-hakemus! kausi "MH1")
          paatos {:hakemusid ah0, :myonnettyavustus 0 :selite "Ei tipu" :paattajanimi "Roope Ankka"}
          ak {:hakemusid     mh1
              :avustuskohdeluokkatunnus "PSA"
              :avustuskohdelajitunnus "1"
              :haettavaavustus 1
              :omarahoitus 1}]

      (h/laheta-hakemus! ah0)
      (h/tarkasta-hakemus! ah0)
      (p/save-paatos! paatos)
      (p/hyvaksy-paatos! ah0)

      (ak/add-avustuskohde! ak)

      (p/find-paatos-pdf mh1) => (partial instance? InputStream)

      (assert-hsl-maksatushakemuspaatos-teksti (:vuosi kausi) "1.1. - 30.6." 1)
      (assert-hsl-maksatushakemuspaatos1-teksti (:vuosi kausi) pdf/today 0 "**" 1)
      (pdf/assert-otsikko "Valtionavustuspäätös" "<päätöspäivämäärä>" "testing"))))

(fact "Maksatushakemus (mh1) - maksuun ei haettu vielä mitään"
  (test-ctx
    (let [kausi (test/next-avattu-empty-hakemuskausi!)
          ah0 (add-hakemus! kausi "AH0")
          mh1 (add-hakemus! kausi "MH1")
          paatos {:hakemusid ah0, :myonnettyavustus 1 :selite nil :paattajanimi "Pentti Päättäjä"}]

      (h/laheta-hakemus! ah0)
      (h/tarkasta-hakemus! ah0)
      (p/save-paatos! paatos)
      (p/hyvaksy-paatos! ah0)

      (p/find-paatos-pdf mh1) => (partial instance? InputStream)

      (assert-hsl-maksatushakemuspaatos-teksti (:vuosi kausi) "1.1. - 30.6." 0)
      (assert-hsl-maksatushakemuspaatos1-teksti (:vuosi kausi) pdf/today 1 0 0)
      (pdf/assert-otsikko "Valtionavustuspäätös" "<päätöspäivämäärä>" "testing"))))

(def percentage (comp p/format-bigdec p/percentage))

(fact "Prosenttilasku"
      (percentage 1M 1M) => "100"
      (percentage 0M 1M) => "0"
      (percentage 1M 2M) => "50"
      (percentage 1M 3M) => "33"
      (percentage 2M 3M) => "67"
      (percentage 5M 9M) => "56"
      (percentage 1M 1000M) => "0.1"
      (percentage 4M 1000M) => "0.4"
      (percentage 5M 1000M) => "0.5")