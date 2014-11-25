(ns juku.db.hakemus_test
  (:require [midje.sweet :refer :all]
            [clj-time.core :as t]
            [juku.db.hakemus :as h]))

(fact
  (let [osastoid 1
        hakemus {:vuosi 2015 :osastoid osastoid
                 :hakuaika {:alkupvm (t/local-date 2013 3 20)
                            :loppupvm (t/local-date 2013 3 20)}}

        id (h/add-hakemus! hakemus)]

    (first (filter #(= (:id %) id) (h/find-osaston-hakemukset osastoid)))
      => (dissoc (assoc hakemus :id id) :osastoid)))