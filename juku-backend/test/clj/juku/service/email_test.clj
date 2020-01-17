(ns juku.service.email-test
  (:require [midje.sweet :refer :all]
            [common.string :as strx]
            [juku.service.hakemus-core :as hc]
            [juku.service.hakemus :as h]
            [juku.service.paatos :as p]
            [juku.service.hakemuskausi :as hk]
            [juku.service.asiahallinta-mock :as asha]
            [juku.service.asiakirjamalli-test :as akmalli-test]
            [juku.service.email-mock :as email]
            [juku.service.test :as test]
            [juku.service.user :as user]))

(akmalli-test/update-test-asiakirjamallit!)

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
          (let [~(symbol "id") (hc/add-hakemus! (hakemus ~hakemustyyppi))] ~@body)))))

(defn assert-message
  ([subject body] (assert-message 0 subject body))
  ([index subject body] (assert-message index #{"petri.sirkkala@solita.fi"} subject body))
  ([index to subject body]
    (fact "sähköpostiviestin tarkastaminen"
      (:to (email/*mock-email* index)) => to
      (:subject (email/*mock-email* index)) => subject
      (:body (email/*mock-email* index)) => (partial strx/substring? body))))

(defn assert-message-count
  ([expected]
   (fact "email amount check"
         (count email/*mock-email*) => expected)))

;; *** Sähköpostiviestit avustushakemuksista ***

(fact "Hakemuksen lähettäminen"
  (test-ctx "AH0"
    (h/laheta-hakemus! id)
    (assert-message-count 2)
    (assert-message (str "Avustushakemus " vuosi " on saapunut")
                    (str "valtionavustushakemuksenne vuodelle " vuosi " on saapunut"))
    (assert-message 1 (str "Helsingin seudun liikenne - avustushakemus " vuosi " on saapunut")
                      (str "valtionavustushakemus vuodelle " vuosi " on saapunut"))))

(fact "Täydennyspyynnön lähettäminen"
  (test-ctx "AH0"
    (h/laheta-hakemus! id)
    (h/taydennyspyynto! id "selite")
    (assert-message-count 3)
    (assert-message 2 (str "Avustushakemus " vuosi " täydennyspyyntö")
                      (str "Joukkoliikenteen valtionavustushakemuksenne vuodelle " vuosi " on palautettu täydennettäväksi"))))

(fact "Täydennyksen lähettäminen"
  (test-ctx "AH0"
    (h/laheta-hakemus! id)
    (h/taydennyspyynto! id "selite")
    (h/laheta-taydennys! id)

    (assert-message-count 5)
    (assert-message 3 (str "Avustushakemus " vuosi " täydennys")
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
    (assert-message-count 3)
    (assert-message 2 (str "Avustuspäätös " vuosi)
                      (str "Joukkoliikenteen valtionavustushakemukseenne vuodelle " vuosi " on tehty päätös"))))

;; *** sähköpostiviestit organisaatiossa useita käyttäjiä ***

(fact "Hakemuksen lähettäminen"
  (test-ctx "AH0"
    (user/update-user! "juku_allekirjoittaja" {:sahkoposti "yes@test.fi"})
    (h/laheta-hakemus! id)

    (assert-message 0 #{"petri.sirkkala@solita.fi" "yes@test.fi"}
                      (str "Avustushakemus " vuosi " on saapunut")
                      (str "valtionavustushakemuksenne vuodelle " vuosi " on saapunut"))
    (assert-message 1 (str "Helsingin seudun liikenne - avustushakemus " vuosi " on saapunut")
                      (str "valtionavustushakemus vuodelle " vuosi " on saapunut"))

    (user/update-user! "juku_allekirjoittaja" {:sahkoposti "petri.sirkkala@solita.fi"})))

(fact "Hakemuksen lähettäminen"
  (test-ctx "AH0"
    (user/update-user! "juku_allekirjoittaja" {:sahkoposti "yes@test.fi" :sahkopostiviestit false})

    (h/laheta-hakemus! id)

    (assert-message (str "Avustushakemus " vuosi " on saapunut")
                    (str "valtionavustushakemuksenne vuodelle " vuosi " on saapunut"))
    (assert-message 1 (str "Helsingin seudun liikenne - avustushakemus " vuosi " on saapunut")
                    (str "valtionavustushakemus vuodelle " vuosi " on saapunut"))

    (user/update-user! "juku_allekirjoittaja" {:sahkoposti "petri.sirkkala@solita.fi" :sahkopostiviestit true})))

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

(fact "Hakemuksen lähettäminen  - ELY"
  (test-ctx "ELY"
    (h/laheta-hakemus! id)
    (assert-message (str "ELY-hakemus " vuosi " on saapunut")
                    (str "ELY-määrärahatarvehakemuksenne vuodelle " vuosi " on saapunut"))))