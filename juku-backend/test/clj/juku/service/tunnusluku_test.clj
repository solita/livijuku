(ns juku.service.tunnusluku-test
  (:require [midje.sweet :refer :all]
            [juku.service.test :as test]
            [juku.service.tunnusluku :as tl]
            [juku.schema.tunnusluku :as tls]
            [common.map :as map]
            [clojure.string :as str]
            [common.collection :as coll]
            [clojure-csv.core :as csv]
            [common.string :as strx]))

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


; *** testit tunnusluvuille joiden dimensio ei ole muotoa: vuosi, organisaatio, sopimustyyppi, extra dimensio

(def test-alue
  (assoc (map/map-values (constantly 1M) tls/Alue)
    :kustannus (map/map-values (constantly 1M) tls/Kustannus)
    :kommentti "testtest"))

(fact "Aluetiedon lisääminen ja haku"
  (test/with-user "juku_hakija" ["juku_hakija"]
    (tl/save-alue! 2016 1 test-alue)
    (tl/find-alue 2016 1) => test-alue))

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

(fact "Kommentin lisääminen ja haku - tyhjä kommentti"
  (test/with-user "juku_hakija" ["juku_hakija"]
    (tl/save-kommentti! 2016 1 "BR" "")
    (tl/find-kommentti 2016 1 "BR") => nil))

(defn test-joukkoliikennetuki [rahamaara]
  (map #(assoc (map/map-values (constantly rahamaara) tls/Joukkoliikennetuki) :avustuskohdeluokkatunnus %) ["PSA", "HK", "K"]))

(fact "Joukkoliikennetuen lisääminen ja haku"
  (test/with-user "juku_hakija" ["juku_hakija"]
    (let [tuki (test-joukkoliikennetuki 1M)]

      (tl/save-joukkoliikennetuki! 2016 1 tuki)
      (set (tl/find-joukkoliikennetuki 2016 1)) => (set tuki))))


; *** csv import ***

(defn import-csv [csv] (tl/import-csv (csv/parse-csv csv :delimiter \;)))

(fact
  "CSV lataus - yksi organisaatio ja yksi rivi"
  (let [text-csv "vuosi;tunnusluku;helsingin seudun liikenne\n2013;Brutto: Nousijat tammikuussa;1\n"]
    (import-csv text-csv) => (partial strx/substring? "Tunnuslukuja ladattiin onnistuneesti: \n- 2013-liikennevuositilasto - 1")
    (:nousut (coll/find-first (coll/eq :kuukausi 1M) (tl/find-liikennevuositilasto 2013 1 "BR"))) => 1M))

(fact
  "CSV lataus - lippuhinta"
  (let [text-csv "vuosi;tunnusluku;helsingin seudun liikenne\n2013;Kertalipun hinta, aikuinen, vyöhyke 1 (€);1"]
    (import-csv text-csv) => (partial strx/substring? "Tunnuslukuja ladattiin onnistuneesti: \n- 2013-lippuhinta - 1")
    (:kertalippuhinta (coll/find-first (coll/eq :vyohykemaara 1M) (tl/find-lippuhinnat 2013 1))) => 1M))

(fact
  "CSV lataus - kaksi organisaatiota ja yksi rivi"
  (let [text-csv "vuosi;tunnusluku;helsingin seudun liikenne;tampere\n2013;Brutto: Nousijat tammikuussa;1;2\n"]
    (import-csv text-csv) => (partial strx/substring?  "Tunnuslukuja ladattiin onnistuneesti: \n- 2013-liikennevuositilasto - 2")
    (:nousut (coll/find-first (coll/eq :kuukausi 1M) (tl/find-liikennevuositilasto 2013 1 "BR"))) => 1M
    (:nousut (coll/find-first (coll/eq :kuukausi 1M) (tl/find-liikennevuositilasto 2013 12 "BR"))) => 2M))

(fact
  "CSV lataus - henkiloautoliikennesuoritteen lataamisongelma"
  (let [header "Vuosi;Tunnusluku;Helsingin seudun liikenne;Hämeenlinna;Lahti;Riihimäki;Hyvinkää;Lappeenranta;Imatra;Kotka;Kouvola;Mikkeli;Savonlinna;Joensuu;Kuopio;Kokkola;Seinäjoki;Vaasa;Jyväskylä;Tampere;Turku;Pori;Rauma;Salo;Oulu;Kajaani;Meri-Lappi;Rovaniemi;Uusimaa ELY;Kaakkois-Suomi ELY;Pohjois-Savo ELY;Etelä-Pohjanmaa ELY;Keski-Suomi ELY;Pirkanmaa ELY;Varsinais-Suomi ELY;Pohjois-Pohjanmaa ELY;Lappi ELY"
        text-csv "2013;Henkilöautoliikenteen suorite  (jos tiedossa);0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;360000000;0;0;937;0;0;0;0;0;0;0;0;0;1988218327;0;0;1370;0;0;0"]
    (import-csv (str header "\n" text-csv)) => (partial strx/substring?  "Tietokantavirheet: \n- 2013-alue-pohjois-savo")
    (tl/find-alue 2013 20) => nil

    (:henkiloautoliikennesuorite (tl/find-alue 2013 14)) => 360000000M
    (:henkiloautoliikennesuorite (tl/find-alue 2013 1)) => nil))

; *** Täyttöaste ***

(fact
  "Aluetiedon 100% täyttöaste"
  (test/with-user "juku_hakija" ["juku_hakija"]
    (let [vuosi (:vuosi (test/next-hakemuskausi!))]

      (tl/save-alue! vuosi 1 test-alue)
      (tl/tayttoaste vuosi 1) => 0.0492M)))

(fact
  "Täyttöaste - tyhjä kausi"
  (test/with-user "juku_hakija" ["juku_hakija"]
    (let [vuosi (:vuosi (test/next-hakemuskausi!))]
      (tl/tayttoaste vuosi 1) => 0M)))

(fact
  "Täyttöaste - lippuhinta"
  (test/with-user "juku_hakija" ["juku_hakija"]
    (let [vuosi (:vuosi (test/next-hakemuskausi!))
          lippuhinnat (map #(assoc (map/map-values (constantly 1M) tls/Lippuhinta) :vyohykemaara %) (range 1M 7M))]

      (tl/save-lippuhinnat! vuosi 1 lippuhinnat)
      (tl/tayttoaste vuosi 1) => (with-precision 3 :rounding HALF_UP (bigdec (/ 1 325))))))

(fact
  "Täyttöaste - kalusto"
  (test/with-user "juku_hakija" ["juku_hakija"]
    (let [vuosi (:vuosi (test/next-hakemuskausi!))
          kalusto (map #(assoc (map/map-values (constantly 1M) tls/Kalusto) :paastoluokkatunnus %)
                           (map #(str "E" %) (range 0M 7M)))]

      (tl/save-kalusto! vuosi 1 "BR" kalusto)
      (tl/tayttoaste vuosi 1) => (with-precision 3 :rounding HALF_UP (bigdec (/ 1 325)))

      (tl/save-kalusto! vuosi 1 "KOS" kalusto)
      (tl/tayttoaste vuosi 1) => (with-precision 3 :rounding HALF_UP (bigdec (/ 2 325))))))