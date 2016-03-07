(ns juku.service.tilastot-test
  (:require [midje.sweet :refer :all]
            [juku.service.test :as test]
            [juku.service.tunnusluku :as tl]
            [juku.service.tunnusluku-test :as tl-test]
            [juku.service.tilastot :as t]
            [juku.schema.tunnusluku :as tls]
            [common.map :as map]
            [clojure.string :as str]
            [juku.service.ely-hakemus-test :as ely-test]
            [common.collection :as coll]
            [juku.service.asiahallinta-mock :as asha]
            [juku.service.hakemus-core :as hc]
            [juku.service.avustuskohde :as ak]
            [juku.service.paatos :as p]
            [juku.service.hakemus :as h]
            [common.core :as c]
            [juku.service.ely-hakemus :as ely]))

; *** Tunnuslukutilastojen testit ***

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
                  (t/tunnusluku-tilasto tunnusluku "ALL" {:vuosi 2013 :sopimustyyppitunnus "BR"} [:organisaatioid :vuosi]))
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

; *** Avustustilasto testit ***

(defn create-test-hakemus [vuosi hakija hakemustyyppitunnus organisaatioid myonnettyavustus]
  (asha/with-asha
    (let [id (hc/add-hakemus! {:vuosi vuosi
                               :hakemustyyppitunnus hakemustyyppitunnus
                               :organisaatioid organisaatioid})
          paatos {:hakemusid id, :myonnettyavustus myonnettyavustus :selite "FooBar"}]
      (test/with-user hakija ["juku_hakija"] (h/laheta-hakemus! id))
      (test/with-user
        "juku_paatoksentekija" ["juku_paatoksentekija"]
        (h/tarkasta-hakemus! id)
        (p/save-paatos! paatos)
        (p/hyvaksy-paatos! id)
      id))))

(defn find-by-vuosi [vuosi tilasto]
  (filter (coll/eq (c/partial-first-arg nth 1) vuosi) tilasto))

(defn insert-test-ah0-hakemus-content [hakemusid]
  (ak/add-avustuskohde! {:hakemusid hakemusid
                         :avustuskohdeluokkatunnus "PSA"
                         :avustuskohdelajitunnus "1"
                         :haettavaavustus 20,
                         :omarahoitus 20})
  (ak/add-avustuskohde! {:hakemusid hakemusid
                         :avustuskohdeluokkatunnus "PSA"
                         :avustuskohdelajitunnus "2"
                         :haettavaavustus 20,
                         :omarahoitus 20}))

(fact
  "Avustustilastot - 1 avustushakemus"
  (let [vuosi (-> (test/next-avattu-empty-hakemuskausi!) :vuosi bigdec)
        id (create-test-hakemus vuosi "juku_hakija" "AH0" 1 30)]

    (insert-test-ah0-hakemus-content id)
    (tl/save-alue! vuosi 1 {:asukasmaara 100})

    (find-by-vuosi vuosi (t/avustus-tilasto "KS1"))
      => [["H" vuosi 40M] ["M" vuosi 30M]]

    (find-by-vuosi vuosi (t/avustus-organisaatio-tilasto "KS1"))
      => [[1M vuosi 40M 30M]]

    (find-by-vuosi vuosi (t/avustus-asukastakohti-tilasto "KS1"))
      => [[1M vuosi 0.3M]]))

(fact
  "Avustustilastot - 2 avustushakemusta"
  (let [vuosi (-> (test/next-avattu-empty-hakemuskausi!) :vuosi bigdec)
        id1 (create-test-hakemus vuosi "juku_hakija" "AH0" 1 30)
        id2 (create-test-hakemus vuosi "juku_hakija_kotka" "AH0" 5 25)]

    (insert-test-ah0-hakemus-content id1)
    (insert-test-ah0-hakemus-content id2)
    (tl/save-alue! vuosi 1 {:asukasmaara 100})
    (tl/save-alue! vuosi 5 {:asukasmaara 100})

    (find-by-vuosi vuosi (t/avustus-tilasto "ALL"))
    => [["H" vuosi 80M] ["M" vuosi 55M]]

    (find-by-vuosi vuosi (t/avustus-organisaatio-tilasto "ALL"))
    => [[1M vuosi 40M 30M] [5M vuosi 40M 25M]]

    (find-by-vuosi vuosi (t/avustus-asukastakohti-tilasto "ALL"))
    => [[1M vuosi 0.3M] [5M vuosi 0.25M]]))

(defn insert-test-ely-hakemus-content [hakemusid]
  (ely-test/insert-maararahatarve! hakemusid (ely-test/maararahatarve-bs 1))
  (ely-test/insert-maararahatarve! hakemusid (ely-test/maararahatarve "KK1"))
  (ely-test/insert-maararahatarve! hakemusid (ely-test/maararahatarve "KK2"))

  (ely-test/insert-kehityshanke! hakemusid {:numero 1M :nimi "testi1234" :arvo 1M :kuvaus "asdf"})
  (ely-test/insert-kehityshanke! hakemusid {:numero 2M :nimi "qwerty" :arvo 2M :kuvaus "asdf"})

  (ely-test/update-elyhakemus! hakemusid ely-test/ely-perustiedot))

(fact
  "Avustustilastot - 1 ely-hakemus"
  (let [vuosi (-> (test/next-avattu-empty-hakemuskausi!) :vuosi bigdec)
        id (create-test-hakemus vuosi "juku_hakija_ely" "ELY" 16 10)]

    (insert-test-ely-hakemus-content id)
    (tl/save-alue! vuosi 16 {:asukasmaara 100})

    (find-by-vuosi vuosi (t/avustus-tilasto "ELY"))
      => [["H" vuosi 13M] ["M" vuosi 10M]]

    (find-by-vuosi vuosi (t/avustus-organisaatio-tilasto "ELY"))
      => [[16M vuosi 13M 10M]]

    (find-by-vuosi vuosi (t/avustus-asukastakohti-tilasto "ELY"))
      => [[16M vuosi 0.1M]]))

(fact
  "Avustustilastot - 2 ely-hakemusta"
  (let [vuosi (-> (test/next-avattu-empty-hakemuskausi!) :vuosi bigdec)
        id1 (create-test-hakemus vuosi "juku_hakija_ely" "ELY" 16 10)
        id2 (create-test-hakemus vuosi "juku_hakija_lappi" "ELY" 24 11)]

    (insert-test-ely-hakemus-content id1)
    (insert-test-ely-hakemus-content id2)
    (tl/save-alue! vuosi 16 {:asukasmaara 100})
    (tl/save-alue! vuosi 24 {:asukasmaara 100})

    (find-by-vuosi vuosi (t/avustus-tilasto "ELY"))
      => [["H" vuosi 26M] ["M" vuosi 21M]]

    (find-by-vuosi vuosi (t/avustus-organisaatio-tilasto "ELY"))
      => [[16M vuosi 13M 10M] [24M vuosi 13M 11M]]

    (find-by-vuosi vuosi (t/avustus-asukastakohti-tilasto "ELY"))
      => [[16M vuosi 0.1M] [24M vuosi 0.11M]]))

(fact
  "Avustustilastot - ely-hakemus + avustushakmeus"
  (let [vuosi (-> (test/next-avattu-empty-hakemuskausi!) :vuosi bigdec)
        id1 (create-test-hakemus vuosi "juku_hakija" "AH0" 1 30)
        id2 (create-test-hakemus vuosi "juku_hakija_ely" "ELY" 16 10)]

    (insert-test-ah0-hakemus-content id1)
    (insert-test-ely-hakemus-content id2)
    (tl/save-alue! vuosi 1 {:asukasmaara 100})
    (tl/save-alue! vuosi 16 {:asukasmaara 100})

    (find-by-vuosi vuosi (t/avustus-tilasto "ELY"))
      => [["H" vuosi 13M] ["M" vuosi 10M]]

    (find-by-vuosi vuosi (t/avustus-tilasto "ALL"))
      => [["H" vuosi 53M] ["M" vuosi 40M]]

    (find-by-vuosi vuosi (t/avustus-organisaatio-tilasto "ELY"))
      => [[16M vuosi 13M 10M]]

    (find-by-vuosi vuosi (t/avustus-organisaatio-tilasto "ALL"))
      => [[1M vuosi 40M 30M] [16M vuosi 13M 10M]]

    (find-by-vuosi vuosi (t/avustus-asukastakohti-tilasto "ELY"))
      => [[16M vuosi 0.1M]]

    (find-by-vuosi vuosi (t/avustus-asukastakohti-tilasto "ALL"))
      => [[1M vuosi 0.3M] [16M vuosi 0.1M]]))

(fact
  "Avustustilastot - ks3"
  (let [vuosi (-> (test/next-avattu-empty-hakemuskausi!) :vuosi bigdec)
        tuki (tl-test/test-joukkoliikennetuki 1M)]

    (tl/save-joukkoliikennetuki! vuosi 33 tuki)
    (tl/save-alue! vuosi 33 {:asukasmaara 10})

    (find-by-vuosi vuosi (t/avustus-tilasto "ALL")) => []
    (find-by-vuosi vuosi (t/avustus-tilasto "KS3")) => [["M" vuosi 3M]]

    (find-by-vuosi vuosi (t/avustus-organisaatio-tilasto "ALL")) => []
    (find-by-vuosi vuosi (t/avustus-organisaatio-tilasto "KS3")) => [[33M vuosi nil 3M]]

    (find-by-vuosi vuosi (t/avustus-asukastakohti-tilasto "ALL")) => []
    (find-by-vuosi vuosi (t/avustus-asukastakohti-tilasto "KS3")) => [[33M vuosi 0.3M]]))

(fact
  "Avustustilastot - 2 x ks3"
  (let [vuosi (-> (test/next-avattu-empty-hakemuskausi!) :vuosi bigdec)]

    (tl/save-joukkoliikennetuki! vuosi 33 (tl-test/test-joukkoliikennetuki 1M))
    (tl/save-alue! vuosi 33 {:asukasmaara 10})

    (tl/save-joukkoliikennetuki! vuosi 25 (tl-test/test-joukkoliikennetuki 2M))
    (tl/save-alue! vuosi 25 {:asukasmaara 20})

    (find-by-vuosi vuosi (t/avustus-tilasto "ALL")) => []
    (find-by-vuosi vuosi (t/avustus-tilasto "KS3")) => [["M" vuosi 9M]]

    (find-by-vuosi vuosi (t/avustus-organisaatio-tilasto "ALL")) => []
    (find-by-vuosi vuosi (t/avustus-organisaatio-tilasto "KS3")) => [[25M vuosi nil 6M] [33M vuosi nil 3M]]

    (find-by-vuosi vuosi (t/avustus-asukastakohti-tilasto "ALL")) => []
    (find-by-vuosi vuosi (t/avustus-asukastakohti-tilasto "KS3")) => [[25M vuosi 0.3M] [33M vuosi 0.3M]]))

(fact
  "Vakiodata 2010 - 2015"
  (tl/save-alue! 2010 1 {:asukasmaara 1})
  (tl/save-alue! 2010 10 {:asukasmaara 1})
  (tl/save-alue! 2010 12 {:asukasmaara 1})
  (tl/save-alue! 2010 13 {:asukasmaara 1})

  (find-by-vuosi 2010 (t/avustus-tilasto "KS1")) => [["H" 2010 7363341] ["M" 2010 7363650]]

  (find-by-vuosi 2010 (t/avustus-organisaatio-tilasto "KS1")) =>
    [[10 2010 320500 320500] [1 2010 5213000 5213309] [12 2010 998530 998530] [13 2010 831311 831311]]

  (find-by-vuosi 2010M (t/avustus-asukastakohti-tilasto "KS1"))
    => [[1M 2010M 5213309M] [10M 2010M 320500M] [12M 2010M 998530M] [13M 2010M 831311M]])

