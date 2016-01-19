(ns juku.service.tunnusluku-test
  (:require [midje.sweet :refer :all]
            [juku.service.test :as test]
            [juku.service.tunnusluku :as tl]
            [juku.schema.tunnusluku :as tls]
            [common.map :as map]))


(fact "Aluetiedon lisääminen ja haku"
  (test/with-user "juku_hakija" ["juku_hakija"]
    (let [alue (assoc (map/map-values (constantly 1M) tls/Alue)
                 :kustannus (map/map-values (constantly 1M) tls/Kustannus))]

      (tl/save-alue! 2016 1 alue)
      (tl/find-alue 2016 1) => alue)))