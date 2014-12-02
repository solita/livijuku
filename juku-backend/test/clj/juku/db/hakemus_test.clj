(ns juku.db.hakemus_test
  (:require [midje.sweet :refer :all]
            [clj-time.core :as t]
            [juku.service.hakemus :as h]))

(defn find-by-id [id] (fn [m] (= (:id m) id)))

(fact
  (let [organisaatioid 1
        hakemus {:vuosi 2015 :hakemustyyppitunnus "AH0" :organisaatioid organisaatioid
                 :hakuaika {:alkupvm (t/local-date 2014 6 1)
                            :loppupvm (t/local-date 2014 12 1)}}

        id (h/add-hakemus! hakemus)]

    (first (filter (find-by-id id) (h/find-organisaation-hakemukset organisaatioid)))
      => (dissoc (assoc hakemus :id id) :organisaatioid)))