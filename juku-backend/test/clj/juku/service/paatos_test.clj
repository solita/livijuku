(ns juku.service.paatos-test
  (:require [midje.sweet :refer :all]
            [clj-time.core :as t]
            [juku.service.hakemus-core :as hc]
            [juku.service.hakemus :as h]
            [juku.service.hakemuskausi :as hk]
            [juku.service.paatos :as p]
            [common.core :as c]
            [juku.service.user :as u]
            [juku.service.asiahallinta-mock :as asha]
            [juku.service.test :as test]
            [clj-http.fake :as fake]))

(def hakemuskausi (test/next-hakemuskausi!))
(def vuosi (:vuosi hakemuskausi))
(hk/update-hakemuskausi-set-diaarinumero! {:vuosi vuosi :diaarinumero (str "dnro:" vuosi)})

(def hsl-ah0-hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid 1M})

(fact "Päätöksen tallentaminen ja hakeminen"
  (test/with-user "juku_kasittelija" ["juku_kasittelija"]
      (let [id (hc/add-hakemus! hsl-ah0-hakemus)
            paatos {:hakemusid id, :myonnettyavustus 1M :selite "FooBar"}]


          (p/save-paatos! paatos)
          (p/find-current-paatos id) => (assoc paatos :paatosnumero 1, :paattaja nil, :paattajanimi nil,
                                                      :poistoaika nil, :voimaantuloaika nil))))

(fact "Päätöksen hyväksyminen"
  (test/with-user "juku_kasittelija" ["juku_kasittelija"]
    (asha/with-asha
      (let [id (hc/add-hakemus! hsl-ah0-hakemus)
            paatos {:hakemusid id, :myonnettyavustus 1M :selite "FooBar"}]

        (p/save-paatos! paatos)

        (test/with-hakija (h/laheta-hakemus! id))
        (h/tarkasta-hakemus! id)
        (p/hyvaksy-paatos! id)

        (:hakemustilatunnus (hc/get-hakemus+ id)) => "P"

        (let [hyvaksytty-paatos (p/find-current-paatos id)]
          (:paattaja hyvaksytty-paatos) => "juku_kasittelija"
          (:voimaantuloaika hyvaksytty-paatos) => c/not-nil?
          (:poistoaika hyvaksytty-paatos) => nil)))))

(fact "Päätöksen hyväksyminen - avointa päätösta ei ole"
  (test/with-user "juku_kasittelija" ["juku_kasittelija"]
    (asha/with-asha
      (let [id (hc/add-hakemus! hsl-ah0-hakemus)]

        (test/with-hakija (h/laheta-hakemus! id))
        (h/tarkasta-hakemus! id)
        (p/hyvaksy-paatos! id) => (throws (str "Hakemuksella " id " ei ole avointa päätöstä"))))))

(fact "Päätöksen hyväksyminen - väärä tila"
  (test/with-user "juku_kasittelija" ["juku_kasittelija"]
    (asha/with-asha
      (let [id (hc/add-hakemus! hsl-ah0-hakemus)
            paatos {:hakemusid id, :myonnettyavustus 1M :selite "FooBar"}]

        (test/with-hakija (h/laheta-hakemus! id))
        (p/save-paatos! paatos)
        (p/hyvaksy-paatos! id) => (throws (str "Hakemuksen (" id
                                               ") päättäminen ei ole sallittu tilassa: V. Hakemuksen päättäminen on sallittu vain tilassa: T"))))))

(fact "Päätöksen peruuttaminen"
  (test/with-user "juku_kasittelija" ["juku_kasittelija"]
    (asha/with-asha-off
      (let [id (hc/add-hakemus! hsl-ah0-hakemus)
            paatos {:hakemusid id, :myonnettyavustus 1M :selite "FooBar"}]

        (p/save-paatos! paatos)

        (test/with-hakija (h/laheta-hakemus! id))
        (h/tarkasta-hakemus! id)
        (p/hyvaksy-paatos! id)
        (p/peruuta-paatos! id)

        (:paatosnumero (p/find-current-paatos id)) => -1

        (:hakemustilatunnus (hc/get-hakemus+ id)) => "T"))))

(fact "Päätöksen hakeminen oletustiedoilla"
  (test/with-user "juku_kasittelija" ["juku_kasittelija"]
      (let [id (hc/add-hakemus! hsl-ah0-hakemus)
            id2 (hc/add-hakemus! hsl-ah0-hakemus)
            paatos {:hakemusid id, :myonnettyavustus 1M :selite "FooBar" :paattajanimi "FooBar"}]

        (let [p (p/find-current-paatos id)]
          (:paatosnumero p) => -1
          (:hakemusid p) => id
          (:myonnettyavustus p) => 0
          (:selite p) => nil)

        (p/save-paatos! paatos)
        (p/find-current-paatos id) => (assoc paatos :paatosnumero 1, :paattaja nil, :paattajanimi "FooBar",
                                                    :poistoaika nil, :voimaantuloaika nil)
        (let [p (p/find-current-paatos id2)]
          (:paatosnumero p) => -1
          (:hakemusid p) => id2
          (:paattajanimi p) => "FooBar"))))