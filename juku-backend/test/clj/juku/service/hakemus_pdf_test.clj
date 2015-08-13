(ns juku.service.hakemus-pdf-test
  (:require [midje.sweet :refer :all]
            [clojure.string :as str]
            [common.collection :as coll]
            [common.map :as m]
            [clj-time.core :as time]
            [clj-time.format :as timef]
            [juku.db.coerce :as dbc]
            [juku.service.pdf-mock :as pdf]
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

(fact "Keskeneräinen avustushakemus"
  (test/with-user "juku_hakija" ["juku_hakija"]
    (asha/with-asha-off
      (pdf/with-mock-pdf
        (let [id (h/add-hakemus! hsl-ah0-hakemus)
              asiakirja (h/find-hakemus-pdf id)]

          asiakirja => (partial instance? InputStream)

          (pdf/assert-otsikko "Valtionavustushakemus" nil)

          (let [teksti (:teksti pdf/*mock-pdf*)]
            teksti => (partial strx/substring? "Hakija: Helsingin seudun liikenne")
            teksti => (partial strx/substring? (str "Hakija hakee vuonna " vuosi " suurten kaupunkiseutujen joukkoliikenteen valtionavustusta 0 euroa."))
            teksti => (partial strx/substring? (str "Hakija osoittaa omaa rahoitusta näihin kohteisiin yhteensä 0 euroa.")))

          (:footer pdf/*mock-pdf*) => (partial strx/substring? "esikatselu - hakemus on keskeneräinen"))))))

(fact "Keskeneräinen avustushakemus - hakemuksella avustuskohde"
  (test/with-user "juku_hakija" ["juku_hakija"]
    (asha/with-asha-off
      (pdf/with-mock-pdf
        (let [id (h/add-hakemus! hsl-ah0-hakemus)]

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

          (let [teksti (:teksti pdf/*mock-pdf*)]
            teksti => (partial strx/substring? "Hakija: Helsingin seudun liikenne")
            teksti => (partial strx/substring? "PSA:n mukaisen liikenteen hankinta")
            teksti => (partial strx/substring? "Paikallisliikenne\t\t\t\t\t1 e")
            teksti => (partial strx/substring? "Integroitupalvelulinja\t\t\t\t\t1 e")
            teksti => (partial strx/substring? "Hintavelvoitteiden korvaaminen")
            teksti => (partial strx/substring? "Seutulippu\t\t\t\t\t1 e")
            teksti => (partial strx/substring? (str "Hakija hakee vuonna " vuosi " suurten kaupunkiseutujen joukkoliikenteen valtionavustusta 3 euroa."))
            teksti => (partial strx/substring? (str "Hakija osoittaa omaa rahoitusta näihin kohteisiin yhteensä 3 euroa.")))

          (:footer pdf/*mock-pdf*) => (partial strx/substring? "esikatselu - hakemus on keskeneräinen"))))))

(fact "Keskeneräinen 1. maksatushakemus"
  (test/with-user "juku_hakija" ["juku_hakija"]
    (asha/with-asha-off
      (pdf/with-mock-pdf
        (let [id (h/add-hakemus! hsl-mh1-hakemus)
              asiakirja (h/find-hakemus-pdf id)]

          asiakirja => (partial instance? InputStream)

          (pdf/assert-otsikko "Valtionavustushakemus" nil)

          (let [teksti (:teksti pdf/*mock-pdf*)]
            teksti => (partial strx/substring? "Hakija: Helsingin seudun liikenne")
            teksti => (partial strx/substring? (str "Hakija hakee vuonna " vuosi
                                                    " suurten kaupunkiseutujen joukkoliikenteen valtionavustuksen maksatusta 0 euroa ajalta 1.1.- 31.6."
                                                    vuosi))
            teksti => (partial strx/substring? (str "Hakija osoittaa omaa rahoitusta näihin kohteisiin yhteensä 0 euroa.")))

          (:footer pdf/*mock-pdf*) => (partial strx/substring? "esikatselu - hakemus on keskeneräinen"))))))