(ns juku.db.hakemus_test
  (:require [midje.sweet :refer :all]
            [clj-time.core :as t]
            [juku.service.hakemus :as h]))

(defn find-by-id [id] (fn [m] (= (:id m) id)))

(fact
  (let [osastoid 1
        hakemus {:vuosi 2015 :nro 1 :osastoid osastoid
                 :hakuaika {:alkupvm (t/local-date 2014 6 1)
                            :loppupvm (t/local-date 2014 12 1)}}

        id (h/add-hakemus! hakemus)]

    (first (filter (find-by-id id) (h/find-osaston-hakemukset osastoid)))
      => (dissoc (assoc hakemus :id id) :osastoid)))