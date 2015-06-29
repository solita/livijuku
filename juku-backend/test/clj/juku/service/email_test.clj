(ns juku.service.email-test
  (:require [midje.sweet :refer :all]
            [clojure.string :as str]
            [common.string :as strx]
            [juku.service.hakemus :as h]
            [juku.service.paatos :as p]
            [juku.service.hakemuskausi :as hk]
            [juku.service.asiahallinta-mock :as asha]
            [juku.service.email-mock :as email]
            [juku.service.test :as test]
            [common.core :as c]))

(def hakemuskausi (test/next-hakemuskausi!))
(def vuosi (:vuosi hakemuskausi))
(asha/with-asha
  (hk/save-hakuohje vuosi "test" "text/plain" (test/inputstream-from  "test"))
  (hk/avaa-hakemuskausi! vuosi))

(defn hakemus [tyyppi] {:vuosi vuosi :hakemustyyppitunnus tyyppi :organisaatioid 1M})

(defmacro test-ctx [hakemustyyppi & body]
  `(test/with-user "juku_hakija" ["juku_hakija"]
     (asha/with-asha
        (email/with-mock-email
          (let [~(symbol "id") (h/add-hakemus! (hakemus ~hakemustyyppi))] ~@body)))))

(defn assert-message [subject body]
  (fact "sähköpostiviestin tarkastaminen"
    (:to email/*mock-email*) => #{"petri.sirkkala@solita.fi"}
    (:subject email/*mock-email*) => subject
    (:body email/*mock-email*) => (partial strx/substring? body)))

;; *** Sähköpostiviestit avustushakemuksista ***

(fact "Hakemuksen lähettäminen"
  (test-ctx "AH0"
    (h/laheta-hakemus! id)
    (assert-message (str "Avustushakemus " vuosi " on saapunut")
                    (str "valtionavustushakemuksenne vuodelle " vuosi " on saapunut"))))

(fact "Täydennyspyynnön lähettäminen"
  (test-ctx "AH0"
    (h/laheta-hakemus! id)
    (h/taydennyspyynto! id "selite")

    (assert-message (str "Avustushakemus " vuosi " täydennyspyyntö")
                    (str "Joukkoliikenteen valtionavustushakemuksenne vuodelle " vuosi " on palautettu täydennettäväksi"))))

(fact "Täydennyksen lähettäminen"
  (test-ctx "AH0"
    (h/laheta-hakemus! id)
    (h/taydennyspyynto! id "selite")
    (h/laheta-taydennys! id)

    (assert-message (str "Avustushakemus " vuosi " täydennys")
                    (str "Joukkoliikenteen valtionavustushakemuksenne täydennys on saapunut"))))

(fact "Päätös"
  (test-ctx "AH0"
    (h/laheta-hakemus! id)
    (h/tarkasta-hakemus! id)
    (p/save-paatos! {:hakemusid id
                     :myonnettyavustus 0
                     :paattajanimi "asdf"
                     :selite nil})

    (p/hyvaksy-paatos! id)

    (assert-message (str "Avustuspäätös " vuosi)
                    (str "Joukkoliikenteen valtionavustushakemukseenne vuodelle " vuosi " on tehty päätös"))))

;; *** Sähköpostiviestit maksatushakemuksista ***

(fact "Hakemuksen lähettäminen - MH1"
  (test-ctx "MH1"
    (h/laheta-hakemus! id)
    (assert-message (str "Maksatushakemus " vuosi "/1 on saapunut")
                    (str "1. maksatushakemuksenne vuodelle " vuosi " on saapunut"))))

(fact "Hakemuksen lähettäminen  - MH2"
  (test-ctx "MH2"
    (h/laheta-hakemus! id)
    (assert-message (str "Maksatushakemus " vuosi "/2 on saapunut")
                    (str "2. maksatushakemuksenne vuodelle " vuosi " on saapunut"))))