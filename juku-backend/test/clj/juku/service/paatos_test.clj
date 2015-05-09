(ns juku.service.paatos-test
  (:require [midje.sweet :refer :all]
            [clj-time.core :as t]
            [juku.service.hakemus :as h]
            [juku.service.paatos :as p]
            [common.core :as c]
            [juku.service.user :as u]
            [juku.service.asiahallinta-mock :as asha]
            [juku.service.test :as test]
            [clj-http.fake :as fake]))

(fact "Päätöksen tallentaminen ja hakeminen"
  (test/with-user "juku_kasittelija" ["juku_kasittelija"]
      (let [hakemuskausi (test/next-hakemuskausi!)
            vuosi (:vuosi hakemuskausi)
            organisaatioid 1
            hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid organisaatioid}

            id (h/add-hakemus! hakemus)
            paatos {:hakemusid id, :myonnettyavustus 1M :selite "FooBar"}]


          (p/save-paatos! paatos)
          (p/find-current-paatos id) => (assoc paatos :paatosnumero 1, :paattaja nil, :poistoaika nil, :voimaantuloaika nil))))

(fact "Päätöksen hyväksyminen"
  (test/with-user "juku_kasittelija" ["juku_kasittelija"]
    (fake/with-fake-routes {#"http://(.+)/hakemus/(.+)/paatos" (asha/asha-handler :paatos "")}

      (let [hakemuskausi (test/next-hakemuskausi!)
            vuosi (:vuosi hakemuskausi)
            organisaatioid 1
            hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid organisaatioid}

            id (h/add-hakemus! hakemus)
            paatos {:hakemusid id, :myonnettyavustus 1M :selite "FooBar"}]


        (p/save-paatos! paatos)
        (p/hyvaksy-paatos! id)

        (:hakemustilatunnus (h/get-hakemus-by-id id)) => "P"

        (let [hyvaksytty-paatos (p/find-current-paatos id)]
          (:paattaja hyvaksytty-paatos) => "juku_kasittelija"
          (:voimaantuloaika hyvaksytty-paatos) => c/not-nil?
          (:poistoaika hyvaksytty-paatos) => nil)))))

(fact "Päätöksen peruuttaminen"
  (test/with-user "juku_kasittelija" ["juku_kasittelija"]
    (asha/with-asha-off
      (let [hakemuskausi (test/next-hakemuskausi!)
            vuosi (:vuosi hakemuskausi)
            organisaatioid 1
            hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid organisaatioid}

            id (h/add-hakemus! hakemus)
            paatos {:hakemusid id, :myonnettyavustus 1M :selite "FooBar"}]


        (p/save-paatos! paatos)
        (p/hyvaksy-paatos! id)
        (p/peruuta-paatos! id)

        (p/find-current-paatos id) => nil
        (:hakemustilatunnus (h/get-hakemus-by-id id)) => "T"))))
