(ns juku.service.tunnusluku-test
  (:require [midje.sweet :refer :all]
            [juku.service.test :as test]
            [juku.service.tunnusluku :as tl]
            [juku.schema.tunnusluku :as tls]
            [common.map :as map]
            [clojure.string :as str]))

(defn test-tunnuslukuservice [name save find data]
  (fact {:midje/description (str "Tunnuslukupalvelutesti - lisääminen ja haku - " name)}
    (test/with-user "juku_hakija" ["juku_hakija"]
      (save 2016 1 "BR" data)
      (set (find 2016 1 "BR")) => (set data))))

(test-tunnuslukuservice
  "liikennevuositilasto" tl/save-liikennevuositilasto! tl/find-liikennevuositilasto
  (map (fn [kk] (assoc (map/map-values (constantly 1M) tls/Liikennetilasto) :kuukausi kk)) (range 1M 13M)))

(test-tunnuslukuservice
  "liikenneviikkotilasto" tl/save-liikenneviikkotilasto! tl/find-liikenneviikkotilasto
  (map #(assoc (map/map-values (constantly 1M) tls/Liikennetilasto) :viikonpaivaluokkatunnus %) ["A" "LA" "SU"]))

(test-tunnuslukuservice
  "kalusto" tl/save-kalusto! tl/find-kalusto
  (map #(assoc (map/map-values (constantly 1M) tls/Kalusto) :paastoluokkatunnus %)
       (map #(str "E" %) (range 0M 7M))))

(test-tunnuslukuservice
  "lipputulo" tl/save-lipputulo! tl/find-lipputulo
  (map #(assoc (map/map-values (constantly 1M) tls/Lipputulo) :kuukausi %) (range 1M 13M)))

(test-tunnuslukuservice
  "liikennöintikorvaus" tl/save-liikennointikorvaus! tl/find-liikennointikorvaus
  (map #(assoc (map/map-values (constantly 1M) tls/Liikennointikorvaus) :kuukausi %) (range 1M 13M)))


; *** testit tunnusluvuille joiden dimensio ei ole muotoa: vuosi, organisaatio, sopimustyyppi [, extra dimensiot]

(fact "Aluetiedon lisääminen ja haku"
  (test/with-user "juku_hakija" ["juku_hakija"]
    (let [alue (assoc (map/map-values (constantly 1M) tls/Alue)
                 :kustannus (map/map-values (constantly 1M) tls/Kustannus)
                 :kommentti "testtest")]

      (tl/save-alue! 2016 1 alue)
      (tl/find-alue 2016 1) => alue)))

(fact "Lippuhinnan lisääminen ja haku"
  (test/with-user "juku_hakija" ["juku_hakija"]
    (let [lippuhinnat (map #(assoc (map/map-values (constantly 1M) tls/Lippuhinta) :vyohykemaara %) (range 1M 7M))]

      (tl/save-lippuhinnat! 2016 1 lippuhinnat)
      (tl/find-lippuhinnat 2016 1) => lippuhinnat)))

(fact "Kommentin lisääminen ja haku"
  (test/with-user "juku_hakija" ["juku_hakija"]
    (let [kommentti (str/join (repeat 40000 "testäöå"))]

      (tl/save-kommentti! 2016 1 "BR" kommentti)
      (tl/find-kommentti 2016 1 "BR") => kommentti)))
