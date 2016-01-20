(ns juku.service.tunnusluku-test
  (:require [midje.sweet :refer :all]
            [juku.service.test :as test]
            [juku.service.tunnusluku :as tl]
            [juku.schema.tunnusluku :as tls]
            [common.map :as map]))

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

(test-tunnuslukuservice
  "lippuhinta" tl/save-lippuhinta! tl/find-lippuhinta
  (map #(assoc (map/map-values (constantly 1M) tls/Lippuhinta) :vyohykelukumaara (first %) :lippuluokkatunnus (second %))
       (for [l (range 1M 7M) lippuluokka ["KE" "AR" "KA" "0"]] [l lippuluokka])))

(fact "Aluetiedon lisääminen ja haku"
  (test/with-user "juku_hakija" ["juku_hakija"]
    (let [alue (assoc (map/map-values (constantly 1M) tls/Alue)
                 :kustannus (map/map-values (constantly 1M) tls/Kustannus))]

      (tl/save-alue! 2016 1 alue)
      (tl/find-alue 2016 1) => alue)))

