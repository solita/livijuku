(ns juku.db.paatos-test
  (:require [midje.sweet :refer :all]
            [clj-time.core :as t]
            [juku.service.hakemus :as h]
            [juku.service.paatos :as p]
            [juku.service.user :as u]))

(fact "Päätöksen tallentaminen ja hakeminen"
  (u/with-user {:tunnus "harri"}
      (let [organisaatioid 1
            hakemus {:vuosi 2015 :hakemustyyppitunnus "AH0" :organisaatioid organisaatioid
                     :hakuaika {:alkupvm (t/local-date 2014 6 1)
                                :loppupvm (t/local-date 2014 12 1)}}

            id (h/add-hakemus! hakemus)
            paatos {:hakemusid id, :myonnettyavustus 1M :selite "FooBar"}]


          (p/save-paatos! paatos)
          (p/find-paatos id) => (assoc paatos :paatosnumero 1, :paattaja nil, :poistoaika nil, :voimaantuloaika nil))))
