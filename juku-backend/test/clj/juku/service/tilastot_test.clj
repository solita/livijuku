(ns juku.service.tilastot-test
  (:require [midje.sweet :refer :all]
            [juku.service.test :as test]
            [juku.service.tunnusluku :as tl]
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

; *** avustustilatot ***

(defn create-test-hakemus [hakija hakemustyyppitunnus organisaatioid myonnettyavustus]
  (test/with-user hakija ["juku_hakija"]
    (asha/with-asha
      (let [hk (test/next-avattu-empty-hakemuskausi!)
            vuosi (:vuosi hk)
            id (hc/add-hakemus! {:vuosi vuosi
                                 :hakemustyyppitunnus hakemustyyppitunnus
                                 :organisaatioid organisaatioid})
            paatos {:hakemusid id, :myonnettyavustus myonnettyavustus :selite "FooBar"}]

        (h/laheta-hakemus! id)
        (h/tarkasta-hakemus! id)
        (p/save-paatos! paatos)
        (p/hyvaksy-paatos! id)

        {:id id :vuosi (bigdec vuosi)}))))

(defn find-by-vuosi [vuosi tilasto]
  (filter (coll/eq (c/partial-first-arg nth 1) vuosi) tilasto))

(fact
  "Avustus yhteenveto tilasto - avustushakemus"
  (let [{:keys [id vuosi]} (create-test-hakemus "juku_hakija" "AH0" 1 30)]
    (ak/add-avustuskohde! {:hakemusid id
                           :avustuskohdeluokkatunnus "PSA"
                           :avustuskohdelajitunnus "1"
                           :haettavaavustus 40,
                           :omarahoitus 40})
    (find-by-vuosi vuosi (t/avustus-tilasto "KS1"))
      => [["H" vuosi 40M] ["M" vuosi 30M]]))

(fact
  "Avustus yhteenveto tilasto - ely-hakemus"
  (let [{:keys [id vuosi]} (create-test-hakemus "juku_hakija_ely" "ELY" 16 10)]

    (ely-test/insert-maararahatarve! id (ely-test/maararahatarve-bs 1))
    (ely-test/insert-maararahatarve! id (ely-test/maararahatarve "KK1"))
    (ely-test/insert-maararahatarve! id (ely-test/maararahatarve "KK2"))

    (ely-test/insert-kehityshanke! id {:numero 1M :nimi "testi1234" :arvo 1M :kuvaus "asdf"})

    (ely-test/update-elyhakemus! id ely-test/ely-perustiedot)

    (find-by-vuosi vuosi (t/avustus-tilasto "ELY"))
      => [["H" vuosi 11M] ["M" vuosi 10M]]))
