(ns juku.service.hakemus-pdf-test
  (:require [midje.sweet :refer :all]
            [juku.service.pdf-mock :as pdf]
            [juku.service.hakemus-core :as hc]
            [juku.service.hakemus :as h]
            [juku.service.hakemuskausi :as hk]
            [juku.service.liitteet :as l]
            [juku.service.avustuskohde :as ak]
            [juku.service.asiahallinta-mock :as asha]
            [juku.service.test :as test]
            [common.string :as strx]
            [juku.service.asiakirjamalli-test :as akmalli-test])
  (:import (java.io InputStream)))

(akmalli-test/update-test-asiakirjamallit!)

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
    (let [id (hc/add-hakemus! hsl-ah0-hakemus)
          pdf (h/find-hakemus-pdf id)]

      (pdf/assert-header "Valtionavustushakemus" nil)
      (assert-hsl-avustushakemus-teksti (pdf/pdf->text pdf) vuosi 0 0)
      (pdf/assert-footer "esikatselu - hakemus on keskeneräinen"))))

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

      (let [pdf (h/find-hakemus-pdf id)
            content (pdf/pdf->text pdf)]
        (pdf/assert-header "Valtionavustushakemus" nil)

        (assert-hsl-avustushakemus-teksti content vuosi "3,1" "3,1")

        content => (partial strx/substring? "PSA:n mukaisen liikenteen hankinta (alv 0%)")
        content => (partial strx/substring? "Paikallisliikenne 1 €")
        content => (partial strx/substring? "Integroitupalvelulinja 1 €")
        content => (partial strx/substring? "Hintavelvoitteiden korvaaminen (alv 10%)")
        content => (partial strx/substring? "Seutulippu 1,1 €")

        (pdf/assert-footer "esikatselu - hakemus on keskeneräinen")))))

(fact "Hakemuksella avustuskohteita - 3 desimaalia ja pyöristäminen"
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

      (let [pdf (h/find-hakemus-pdf id)
            content (pdf/pdf->text pdf)]

        (pdf/assert-header "Valtionavustushakemus" nil)
        (assert-hsl-avustushakemus-teksti content vuosi "4,73" "4,73")

        content => (partial strx/substring? "PSA:n mukaisen liikenteen hankinta (alv 0%)")
        content => (partial strx/substring? "Paikallisliikenne 1,12 €")
        content => (partial strx/substring? "Integroitupalvelulinja 1,13 €")
        content => (partial strx/substring? "Hintavelvoitteiden korvaaminen (alv 10%)")
        content => (partial strx/substring? "Seutulippu 1,24 €")
        content => (partial strx/substring? "Kaupunkilippu tai kuntalippu 1,24 €")

        (pdf/assert-footer "esikatselu - hakemus on keskeneräinen")))))

(fact "Hakemuksella on liite"
  (test-ctx
    (let [id (hc/add-hakemus! hsl-ah0-hakemus)
          liite {:hakemusid id :nimi "test" :contenttype "text/plain"}]

      (l/add-liite! liite (test/inputstream-from "test"))

      (let [pdf (h/find-hakemus-pdf id)
            content (pdf/pdf->text pdf)]

        (pdf/assert-header "Valtionavustushakemus" nil)
        (assert-hsl-avustushakemus-teksti content vuosi 0 0)
        content => (partial strx/substring? "test")))))

(fact "Hakemuksella on liitteitä"
  (test-ctx
    (let [id (hc/add-hakemus! hsl-ah0-hakemus)
          l1 {:hakemusid id :nimi "test1.txt" :contenttype "text/plain"}
          l2 {:hakemusid id :nimi "test2.txt" :contenttype "text/plain"}]

      (l/add-liite! l1 (test/inputstream-from "test"))
      (l/add-liite! l2 (test/inputstream-from "test"))

      (let [pdf (h/find-hakemus-pdf id)
            content (pdf/pdf->text pdf)]

        (pdf/assert-header "Valtionavustushakemus" nil)
        (assert-hsl-avustushakemus-teksti content vuosi 0 0)

        content => (partial strx/substring? "test1.txt")
        content => (partial strx/substring? "test2.txt"))))))

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

      (let [pdf (h/find-hakemus-pdf id)
            content (pdf/pdf->text pdf)]

        (pdf/assert-header "Valtionavustushakemus" nil)

        (assert-hsl-avustushakemus-teksti content vuosi "20 000" "20 000")

        content => (partial strx/substring? "PSA:n mukaisen liikenteen hankinta")
        content => (partial strx/substring? "Paikallisliikenne 10 000 €")
        content => (partial strx/substring? "Integroitupalvelulinja 10 000 €"))

        (pdf/assert-footer "esikatselu - hakemus on keskeneräinen"))))

(fact "Lähetetty hakemus"
  (test-ctx
    (let [id (hc/add-hakemus! hsl-ah0-hakemus)]

      (h/laheta-hakemus! id)

      (let [pdf (h/find-hakemus-pdf id)
            content (pdf/pdf->text pdf)]

        (pdf/assert-header "Valtionavustushakemus" "testing")
        (assert-hsl-avustushakemus-teksti content vuosi 0 0)
        (pdf/assert-footer hc/kasittelija-organisaatio-name)))))

(fact "Keskeneräinen 1. maksatushakemus"
  (test-ctx
    (let [id (hc/add-hakemus! hsl-mh1-hakemus)
          pdf (h/find-hakemus-pdf id)
          content (pdf/pdf->text pdf)]

      (pdf/assert-header "Valtionavustushakemus" nil)
      (assert-hsl-maksatushakemus-teksti content vuosi 0 0)
      (pdf/assert-footer "esikatselu - hakemus on keskeneräinen"))))

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

      (pdf/assert-header "Valtionavustushakemus" nil)
      (pdf/assert-footer hc/kasittelija-organisaatio-name)

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

      (pdf/assert-header "Valtionavustushakemus" nil)
      (pdf/assert-footer hc/kasittelija-organisaatio-name)

      (let [text (pdf/pdf->text (h/find-hakemus-pdf id))]

        (assert-hsl-maksatushakemus-teksti text vuosi "10 001,24" "13 001,24")

        text => #(not (strx/substring? %
                  "Yhteensä kaikkiin kohteisiin hakija on osoittanut omaa rahoitusta"))

        text => (partial strx/substring? "PSA:n mukaisen liikenteen hankinta (alv 0%)")
        text => (partial strx/substring? "Paikallisliikenne 5 000 €")
        text => (partial strx/substring? "Integroitupalvelulinja 5 000 €")
        text => (partial strx/substring? "Hintavelvoitteiden korvaaminen (alv 10%)")
        text => (partial strx/substring? "Kaupunkilippu tai kuntalippu 1,24 €")))))