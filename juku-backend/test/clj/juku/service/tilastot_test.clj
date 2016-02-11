(ns juku.service.tilastot-test
  (:require [midje.sweet :refer :all]
            [juku.service.test :as test]
            [juku.service.tunnusluku :as tl]
            [juku.service.tilastot :as t]
            [juku.schema.tunnusluku :as tls]
            [common.map :as map]
            [clojure.string :as str]
            [common.collection :as coll]))

(def test-liikennevuositilasto
  (map (fn [kk] (assoc (map/map-values (constantly 1M) tls/Liikennetilasto) :kuukausi kk)) (range 1M 13M)))

(def test-liikenneviikkotilasto
  (map #(assoc (map/map-values (constantly 1M) tls/Liikennetilasto) :viikonpaivaluokkatunnus %) ["A" "LA" "SU"]))

(defn test-tunnuslukuservice [tunnusluku save data result]
  (fact {:midje/description (str "Tunnuslukutilastotesti - lisääminen ja haku - " (name tunnusluku))}
        (test/with-user
          "juku_hakija" ["juku_hakija"]
          (save 2013 1 "BR" data)
          (filter (coll/eq first 1M)
                  (t/tunnusluku-tilasto tunnusluku "ALL" {:vuosi 2013} [:organisaatioid :vuosi]))
            => [[1M 2013M result]])))

(test-tunnuslukuservice :nousut tl/save-liikennevuositilasto! test-liikennevuositilasto 12M)
(test-tunnuslukuservice :lahdot tl/save-liikennevuositilasto! test-liikennevuositilasto 12M)
(test-tunnuslukuservice :linjakilometrit tl/save-liikennevuositilasto! test-liikennevuositilasto 12M)

(test-tunnuslukuservice :nousut-viikko tl/save-liikenneviikkotilasto! test-liikenneviikkotilasto 3M)
(test-tunnuslukuservice :lahdot-viikko tl/save-liikenneviikkotilasto! test-liikenneviikkotilasto 3M)
(test-tunnuslukuservice :linjakilometrit-viikko tl/save-liikenneviikkotilasto! test-liikenneviikkotilasto 3M)

(test-tunnuslukuservice :kalusto tl/save-kalusto!
                        (map #(assoc (map/map-values (constantly 1M) tls/Kalusto) :paastoluokkatunnus %)
                             (map #(str "E" %) (range 0M 7M)))
                        7M)

(test-tunnuslukuservice :lipputulo tl/save-lipputulo!
                        (map #(assoc (map/map-values (constantly 1M) tls/Lipputulo) :kuukausi %) (range 1M 13M))
                        48M)

(test-tunnuslukuservice :liikennointikorvaus tl/save-liikennointikorvaus!
                        (map #(assoc (map/map-values (constantly 1M) tls/Liikennointikorvaus) :kuukausi %) (range 1M 13M))
                        12M)

; *** testit tunnusluvuille joiden dimensio ei ole muotoa: vuosi, organisaatio, sopimustyyppi, extra dimensio

(fact "Lippuhinnan lisääminen ja lippuhintatilasto"
  (test/with-user "juku_hakija" ["juku_hakija"]
    (let [lippuhinnat (map #(assoc (map/map-values (constantly 1M) tls/Lippuhinta) :vyohykemaara %) (range 1M 7M))]

      (tl/save-lippuhinnat! 2013 1 lippuhinnat)
      (filter (coll/eq first 1M)
              (t/tunnusluku-tilasto :lippuhinnat "ALL" {:vuosi 2013} [:organisaatioid :vuosi]))
        => [[1M 2013M 12M]])))

(fact "Aluetiedon lisääminen ja kustannustilasto"
  (test/with-user "juku_hakija" ["juku_hakija"]
    (let [alue (assoc (map/map-values (constantly 1M) tls/Alue)
                 :kustannus (map/map-values (constantly 1M) tls/Kustannus)
                 :kommentti "testtest")]

      (tl/save-alue! 2013 1 alue)
      (filter (coll/eq first 1M)
              (t/tunnusluku-tilasto :kustannukset "ALL" {:vuosi 2013} [:organisaatioid :vuosi]))
        => [[1M 2013M 5M]])))


