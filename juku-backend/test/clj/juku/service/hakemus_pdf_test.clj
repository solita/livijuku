(ns juku.service.hakemus-pdf-test
  (:require [midje.sweet :refer :all]
            [clojure.string :as str]
            [common.collection :as coll]
            [common.map :as m]
            [clj-time.core :as time]
            [clj-time.format :as timef]
            [juku.db.coerce :as dbc]
            [juku.service.pdf-mock :as pdf]
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
(def hsl-mh1-hakemus {:vuosi vuosi :hakemustyyppitunnus "MH1" :organisaatioid 1M})

(defmacro test-ctx [& body]
  `(test/with-user "juku_hakija" ["juku_hakija"]
     (asha/with-asha
       (pdf/with-mock-pdf ~@body))))

(defn assert-hsl-avustushakemus-teksti
  ([haettuavustus omarahoitus] (assert-hsl-avustushakemus-teksti (:teksti pdf/*mock-pdf*) vuosi haettuavustus omarahoitus))
  ([teksti vuosi haettuavustus omarahoitus]
    (fact
      "HSL avustushakemusdokumentin yleisen sisällön tarkastaminen"

      teksti => (partial strx/substring? "Hakija: Helsingin seudun liikenne")
      teksti => (partial strx/substring? (str "Hakija hakee vuonna " vuosi
                                              " suurten kaupunkiseutujen joukkoliikenteen valtionavustusta "
                                              haettuavustus " euroa."))
      teksti => (partial strx/substring? (str "Hakija osoittaa omaa rahoitusta näihin kohteisiin yhteensä "
                                              omarahoitus " euroa.")))))


(defn assert-hsl-maksatushakemus-teksti
  ([haettuavustus omarahoitus] (assert-hsl-maksatushakemus-teksti (:teksti pdf/*mock-pdf*) vuosi haettuavustus omarahoitus))
  ([teksti vuosi haettuavustus omarahoitus]
    (fact "HSL maksatushakemusdokumentin yleisen sisällön tarkastaminen"
      teksti => (partial strx/substring? "Hakija: Helsingin seudun liikenne")
      teksti => (partial strx/substring? (str "Hakija hakee vuonna " vuosi
                                              " suurten kaupunkiseutujen joukkoliikenteen valtionavustuksen maksatusta " haettuavustus " euroa ajalta 1.1.- 30.6."
                                              vuosi))
      teksti => (partial strx/substring? (str "Hakija osoittaa omaa rahoitusta näihin kohteisiin yhteensä " omarahoitus " euroa.")))))

(fact "Keskeneräinen avustushakemus"

(fact "Ei avustuskohteita"
  (test-ctx
    (let [id (hc/add-hakemus! hsl-ah0-hakemus)]

      (h/find-hakemus-pdf id) => (partial instance? InputStream)

      (pdf/assert-otsikko "Valtionavustushakemus" nil)

      (assert-hsl-avustushakemus-teksti 0 0)

      (:footer pdf/*mock-pdf*) => (partial strx/substring? "esikatselu - hakemus on keskeneräinen"))))

(fact "Hakemuksella avustuskohteita"
  (test-ctx
    (let [id (hc/add-hakemus! hsl-ah0-hakemus)]

      (ak/add-avustuskohde! {:hakemusid id
                             :avustuskohdeluokkatunnus "PSA"
                             :avustuskohdelajitunnus "1"
                             :haettavaavustus 1,
                             :omarahoitus 1})

      (ak/add-avustuskohde! {:hakemusid id
                             :avustuskohdeluokkatunnus "PSA"
                             :avustuskohdelajitunnus "2"
                             :haettavaavustus 1,
                             :omarahoitus 1})

      (ak/add-avustuskohde! {:hakemusid id
                             :avustuskohdeluokkatunnus "HK"
                             :avustuskohdelajitunnus "SL"
                             :haettavaavustus 1,
                             :omarahoitus 1})

      (h/find-hakemus-pdf id) => (partial instance? InputStream)

      (pdf/assert-otsikko "Valtionavustushakemus" nil)

      (assert-hsl-avustushakemus-teksti "3,1" "3,1")

      (let [teksti (:teksti pdf/*mock-pdf*)]
        teksti => (partial strx/substring? "PSA:n mukaisen liikenteen hankinta (alv 0%)")
        teksti => (partial strx/substring? "Paikallisliikenne\t\t\t\t\t1 €")
        teksti => (partial strx/substring? "Integroitupalvelulinja\t\t\t\t\t1 €")
        teksti => (partial strx/substring? "Hintavelvoitteiden korvaaminen (alv 10%)")
        teksti => (partial strx/substring? "Seutulippu\t\t\t\t\t1,1 €"))

      (:footer pdf/*mock-pdf*) => (partial strx/substring? "esikatselu - hakemus on keskeneräinen"))))

(fact "Hakemuksella avustuskohteita - 3 desimaalia ja pyöristmäinen"
  (test-ctx
    (let [id (hc/add-hakemus! hsl-ah0-hakemus)]

      (ak/add-avustuskohde! {:hakemusid id
                             :avustuskohdeluokkatunnus "PSA"
                             :avustuskohdelajitunnus "1"
                             :haettavaavustus 1.124,
                             :omarahoitus 1.124})

      (ak/add-avustuskohde! {:hakemusid id
                             :avustuskohdeluokkatunnus "PSA"
                             :avustuskohdelajitunnus "2"
                             :haettavaavustus 1.125,
                             :omarahoitus 1.125})

      (ak/add-avustuskohde! {:hakemusid id
                             :avustuskohdeluokkatunnus "HK"
                             :avustuskohdelajitunnus "SL"
                             :haettavaavustus 1.124,
                             :omarahoitus 1.124})

      (ak/add-avustuskohde! {:hakemusid id
                             :avustuskohdeluokkatunnus "HK"
                             :avustuskohdelajitunnus "KL"
                             :haettavaavustus 1.125,
                             :omarahoitus 1.125})

      (h/find-hakemus-pdf id) => (partial instance? InputStream)

      (pdf/assert-otsikko "Valtionavustushakemus" nil)

      (assert-hsl-avustushakemus-teksti "4,73" "4,73")

      (let [teksti (:teksti pdf/*mock-pdf*)]
        teksti => (partial strx/substring? "PSA:n mukaisen liikenteen hankinta (alv 0%)")
        teksti => (partial strx/substring? "Paikallisliikenne\t\t\t\t\t1,12 €")
        teksti => (partial strx/substring? "Integroitupalvelulinja\t\t\t\t\t1,13 €")
        teksti => (partial strx/substring? "Hintavelvoitteiden korvaaminen (alv 10%)")
        teksti => (partial strx/substring? "Seutulippu\t\t\t\t\t1,24 €")
        teksti => (partial strx/substring? "Kaupunkilippu tai kuntalippu\t\t\t\t\t1,24 €"))

      (:footer pdf/*mock-pdf*) => (partial strx/substring? "esikatselu - hakemus on keskeneräinen"))))

(fact "Hakemuksella on liite"
  (test-ctx
    (let [id (hc/add-hakemus! hsl-ah0-hakemus)
          liite {:hakemusid id :nimi "test" :contenttype "text/plain"}]

      (l/add-liite! liite (test/inputstream-from "test"))

      (h/find-hakemus-pdf id) => (partial instance? InputStream)

      (pdf/assert-otsikko "Valtionavustushakemus" nil)

      (assert-hsl-avustushakemus-teksti 0 0)

      (:teksti pdf/*mock-pdf*) => (partial strx/substring? "test"))))

(fact "Hakemuksella on liitteitä"
  (test-ctx
    (let [id (hc/add-hakemus! hsl-ah0-hakemus)
          l1 {:hakemusid id :nimi "test1.txt" :contenttype "text/plain"}
          l2 {:hakemusid id :nimi "test2.txt" :contenttype "text/plain"}]

      (l/add-liite! l1 (test/inputstream-from "test"))
      (l/add-liite! l2 (test/inputstream-from "test"))

      (h/find-hakemus-pdf id) => (partial instance? InputStream)

      (pdf/assert-otsikko "Valtionavustushakemus" nil)

      (assert-hsl-avustushakemus-teksti 0 0)
      (let [teksti (:teksti pdf/*mock-pdf*)]
        teksti => (partial strx/substring? "test1.txt")
        teksti => (partial strx/substring? "test2.txt")))))

(fact "Hakemuksella avustuskohteita - iso rahasumma"
  (test-ctx
    (let [id (hc/add-hakemus! hsl-ah0-hakemus)]

      (ak/add-avustuskohde! {:hakemusid id
                             :avustuskohdeluokkatunnus "PSA"
                             :avustuskohdelajitunnus "1"
                             :haettavaavustus 10000,
                             :omarahoitus 10000})

      (ak/add-avustuskohde! {:hakemusid id
                             :avustuskohdeluokkatunnus "PSA"
                             :avustuskohdelajitunnus "2"
                             :haettavaavustus 10000,
                             :omarahoitus 10000})

      (h/find-hakemus-pdf id) => (partial instance? InputStream)

      (pdf/assert-otsikko "Valtionavustushakemus" nil)

      (assert-hsl-avustushakemus-teksti "20 000" "20 000")

      (let [teksti (:teksti pdf/*mock-pdf*)]
        teksti => (partial strx/substring? "PSA:n mukaisen liikenteen hankinta")
        teksti => (partial strx/substring? "Paikallisliikenne\t\t\t\t\t10 000 €")
        teksti => (partial strx/substring? "Integroitupalvelulinja\t\t\t\t\t10 000 €"))

      (:footer pdf/*mock-pdf*) => (partial strx/substring? "esikatselu - hakemus on keskeneräinen")))))

(fact "Lähetetty hakemus"
  (test-ctx
    (let [id (hc/add-hakemus! hsl-ah0-hakemus)]

      (h/laheta-hakemus! id)

      (h/find-hakemus-pdf id) => (partial instance? InputStream)

      (pdf/assert-otsikko "Valtionavustushakemus" "testing")

      (assert-hsl-avustushakemus-teksti 0 0)

      (:footer pdf/*mock-pdf*) => (partial strx/substring? hc/kasittelija-organisaatio-name))))

(fact "Keskeneräinen 1. maksatushakemus"
  (test-ctx
    (let [id (hc/add-hakemus! hsl-mh1-hakemus)
          asiakirja (h/find-hakemus-pdf id)]

      asiakirja => (partial instance? InputStream)

      (pdf/assert-otsikko "Valtionavustushakemus" nil)

      (assert-hsl-maksatushakemus-teksti 0 0)

      (:footer pdf/*mock-pdf*) => (partial strx/substring? "esikatselu - hakemus on keskeneräinen"))))

(fact
  "Maksatushakemuksella avustuskohteita joilla kirjattu vain omaa rahoitusta - LIVIJUKU-1013"
  (test-ctx
    (let [id (hc/add-hakemus! hsl-mh1-hakemus)]

      (ak/add-avustuskohde! {:hakemusid id
                             :avustuskohdeluokkatunnus "PSA"
                             :avustuskohdelajitunnus "1"
                             :haettavaavustus 5000,
                             :omarahoitus 5000})

      (ak/add-avustuskohde! {:hakemusid id
                             :avustuskohdeluokkatunnus "PSA"
                             :avustuskohdelajitunnus "2"
                             :haettavaavustus 5000,
                             :omarahoitus 8000})

      (ak/add-avustuskohde! {:hakemusid id
                             :avustuskohdeluokkatunnus "HK"
                             :avustuskohdelajitunnus "SL"
                             :haettavaavustus 0,
                             :omarahoitus 1000})

      (ak/add-avustuskohde! {:hakemusid id
                             :avustuskohdeluokkatunnus "HK"
                             :avustuskohdelajitunnus "KL"
                             :haettavaavustus 1.125,
                             :omarahoitus 1.125})


      (h/laheta-hakemus! id)

      (pdf/assert-otsikko "Valtionavustushakemus" nil)
      (:footer pdf/*mock-pdf*) => hc/kasittelija-organisaatio-name

      (let [text (pdf/pdf->text (h/find-hakemus-pdf id))]

        (assert-hsl-maksatushakemus-teksti text vuosi "10 001,24" "13 001,24")

        text => (partial strx/substring? "Yhteensä kaikkiin kohteisiin hakija on osoittanut omaa rahoitusta 14 101,24 euroa")

        text => (partial strx/substring? "PSA:n mukaisen liikenteen hankinta (alv 0%)")
        text => (partial strx/substring? "Paikallisliikenne 5 000 €")
        text => (partial strx/substring? "Integroitupalvelulinja 5 000 €")
        text => (partial strx/substring? "Hintavelvoitteiden korvaaminen (alv 10%)")
        text => (partial strx/substring? "Kaupunkilippu tai kuntalippu 1,24 €")))))

(fact
  "Maksatushakemuksella ei ole avustuskohteita joilla kirjattu vain omaa rahoitusta - LIVIJUKU-1020"
  (test-ctx
    (let [id (hc/add-hakemus! hsl-mh1-hakemus)]

      (ak/add-avustuskohde! {:hakemusid id
                             :avustuskohdeluokkatunnus "PSA"
                             :avustuskohdelajitunnus "1"
                             :haettavaavustus 5000,
                             :omarahoitus 5000})

      (ak/add-avustuskohde! {:hakemusid id
                             :avustuskohdeluokkatunnus "PSA"
                             :avustuskohdelajitunnus "2"
                             :haettavaavustus 5000,
                             :omarahoitus 8000})

      (ak/add-avustuskohde! {:hakemusid id
                             :avustuskohdeluokkatunnus "HK"
                             :avustuskohdelajitunnus "KL"
                             :haettavaavustus 1.125,
                             :omarahoitus 1.125})


      (h/laheta-hakemus! id)

      (pdf/assert-otsikko "Valtionavustushakemus" nil)
      (:footer pdf/*mock-pdf*) => hc/kasittelija-organisaatio-name

      (let [text (pdf/pdf->text (h/find-hakemus-pdf id))]

        (assert-hsl-maksatushakemus-teksti text vuosi "10 001,24" "13 001,24")

        text => #(not (strx/substring? %
                  "Yhteensä kaikkiin kohteisiin hakija on osoittanut omaa rahoitusta"))

        text => (partial strx/substring? "PSA:n mukaisen liikenteen hankinta (alv 0%)")
        text => (partial strx/substring? "Paikallisliikenne 5 000 €")
        text => (partial strx/substring? "Integroitupalvelulinja 5 000 €")
        text => (partial strx/substring? "Hintavelvoitteiden korvaaminen (alv 10%)")
        text => (partial strx/substring? "Kaupunkilippu tai kuntalippu 1,24 €")))))