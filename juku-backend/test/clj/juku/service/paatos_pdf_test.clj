(ns juku.service.hakemus-pdf-test
  (:require [midje.sweet :refer :all]
            [clojure.string :as str]
            [common.collection :as coll]
            [common.map :as m]
            [clj-time.core :as time]
            [clj-time.format :as timef]
            [juku.db.coerce :as dbc]
            [juku.service.pdf-mock :as pdf]
            [juku.service.paatos :as p]
            [juku.service.hakemus :as h]
            [juku.service.hakemuskausi :as hk]
            [juku.service.liitteet :as l]
            [juku.service.avustuskohde :as ak]
            [juku.service.asiahallinta-mock :as asha]
            [juku.service.test :as test]
            [juku.headers :as headers]
            [common.core :as c]
            [common.string :as strx]))

(defn find-by-id [id] (fn [m] (= (:id m) id)))

(def hakemuskausi (test/next-hakemuskausi!))
(def vuosi (:vuosi hakemuskausi))
(hk/update-hakemuskausi-set-diaarinumero! {:vuosi vuosi :diaarinumero (str "dnro:" vuosi)})

(def hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid 1M})

(fact "Päätöksen esikatselu"
  (test/with-user "juku_hakija" ["juku_hakija"]
    (asha/with-asha-off
      (pdf/with-mock-pdf
        (let [id (h/add-hakemus! hakemus)
              asiakirja (p/paatos-pdf id)]

          asiakirja => c/not-nil?

          (pdf/assert-otsikko "Valtionavustuspäätös" nil)

          (let [teksti (:teksti pdf/*mock-pdf*)]
            teksti => (partial strx/substring? "Hakija: Helsingin seudun liikenne")
            teksti => (partial strx/substring? (str "Hakija hakee vuonna " vuosi " suurten kaupunkiseutujen joukkoliikenteen valtionavustusta 0 euroa."))
            teksti => (partial strx/substring? (str "Hakija osoittaa omaa rahoitusta näihin kohteisiin yhteensä 0 euroa."))

            teksti => (partial strx/substring? (str "seurantatiedot ajalta 1.1. - 31.6." vuosi " päivämäärään 31.8." vuosi " mennessä."))
            teksti => (partial strx/substring? (str " seurantatiedot ajalta 1.7. - 31.12." vuosi " päivämäärään 31.1." (+ vuosi 1) " mennessä.")))

          (:footer pdf/*mock-pdf*) => (partial strx/substring? "Liikennevirasto"))))))