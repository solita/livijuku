(ns juku.service.paatos-test
  (:require [midje.sweet :refer :all]
            [clj-time.core :as t]
            [juku.service.hakemus :as h]
            [juku.service.paatos :as p]
            [juku.service.user :as u]
            [juku.service.test :as test]))

(fact "Päätöksen tallentaminen ja hakeminen"
  (u/with-user {:tunnus "harri"}
      (let [hakemuskausi (test/next-hakemuskausi!)
            vuosi (:vuosi hakemuskausi)
            organisaatioid 1
            hakemus {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid organisaatioid}

            id (h/add-hakemus! hakemus)
            paatos {:hakemusid id, :myonnettyavustus 1M :selite "FooBar"}]


          (p/save-paatos! paatos)
          (p/find-paatos id) => (assoc paatos :paatosnumero 1, :paattaja nil, :poistoaika nil, :voimaantuloaika nil))))
