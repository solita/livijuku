(ns juku.service.hakemuskausi-test
  (:require [midje.sweet :refer :all]
            [juku.service.hakemuskausi :as hk]
            [juku.service.hakemus :as h]
            [common.collection :as c]
            [common.string :as str]
            [common.map :as m]
            [juku.service.test :as test]
            [juku.service.asiahallinta-mock :as asha]
            [clj-time.core :as time]
            [clj-http.fake :as fake])
  (:import (java.io ByteArrayInputStream)))

(def hakemuskausi (test/next-hakemuskausi!))
(def vuosi (:vuosi hakemuskausi))

(defn find-hakemuskausi+ [vuosi] (c/find-first (c/eq :vuosi vuosi) (hk/find-hakemuskaudet+summary)))

(defn inputstream-from [txt] (ByteArrayInputStream. (.getBytes txt)))

(defn assert-avustuskohteet [vuosi]
  (fact (str "Test avustuskohteet on luotu oikein avauksen jälkeen vuodelle: " vuosi)
    (let [stats (m/map-values first (group-by :lajitunnus (test/select-akohde-amounts-broup-by-organisaatiolaji {:vuosi vuosi})))
          aklaji-count (:amount (first (test/select-count-akohdelaji)))
          distinct (map :distinctvalues (vals stats))]
      (get-in stats ["KS1" :akohdeamount]) => aklaji-count
      (get-in stats ["KS2" :akohdeamount]) => (- aklaji-count 1)
      (get-in stats ["ELY" :akohdeamount]) => (- aklaji-count 1)
      (get-in stats ["LV" :akohdeamount]) => nil
      distinct => [1M 1M 1M])))

(facts "-- Hakemuskauden hallinta - avaaminen ja sulkeminen --"

(test/with-user "juku_kasittelija" ["juku_kasittelija"]
    (fact "Avaa hakemuskausi"
      (asha/with-asha
        (let [vuosi (:vuosi (test/next-hakemuskausi!))]
          (hk/save-hakuohje vuosi "test" "text/plain" (inputstream-from  "test"))
          (hk/avaa-hakemuskausi! vuosi)

          (assert-avustuskohteet vuosi)
          (:diaarinumero (h/find-hakemuskausi {:vuosi vuosi})) => "testing"
          (asha/headers :avaus) => asha/valid-headers?
          (:content (first (:multipart (asha/request :avaus)))) =>
            (str "{\"omistavaHenkilo\":\"test\",\"omistavaOrganisaatio\":\"Liikennevirasto\",\"asianNimi\":\"Hakemuskausi " vuosi "\"}"))))

    (fact "Sulje hakemuskausi"
      (asha/with-asha
        (let [vuosi (:vuosi (test/next-hakemuskausi!))]
          (hk/save-hakuohje vuosi "test" "text/plain" (inputstream-from  "test"))
          (hk/avaa-hakemuskausi! vuosi)
          (hk/sulje-hakemuskausi! vuosi)

          (asha/headers :sulkeminen) => asha/valid-headers?
          (:uri (asha/request :sulkeminen)) => (partial str/substring? "hakemuskausi/testing/sulje"))))))

(facts "-- Hakemuskauden hallinta - avaaminen ja sulkeminen - asiahallinta kytketty pois päältä--"

(test/with-user "juku_kasittelija" ["juku_kasittelija"]
  (asha/with-asha-off
    (fact "Avaa hakemuskausi"
      (let [vuosi (:vuosi (test/next-hakemuskausi!))]
          (hk/save-hakuohje vuosi "test" "text/plain" (inputstream-from  "test"))
          (hk/avaa-hakemuskausi! vuosi)

          (assert-avustuskohteet vuosi)
          (:diaarinumero (h/find-hakemuskausi {:vuosi vuosi})) => nil)

    (fact "Sulje hakemuskausi"
      (let [vuosi (:vuosi (test/next-hakemuskausi!))]
        (hk/save-hakuohje vuosi "test" "text/plain" (inputstream-from  "test"))
        (hk/avaa-hakemuskausi! vuosi)
        (hk/sulje-hakemuskausi! vuosi)))))))

(fact "Uuden hakuohjeen tallentaminen ja hakeminen"
  (let [hakuohje {:vuosi vuosi :nimi "test" :contenttype "text/plain"}]

    (hk/save-hakuohje vuosi "test" "text/plain" (inputstream-from  "test"))
    (slurp (:sisalto (hk/find-hakuohje-sisalto vuosi))) => "test"))

(fact "Hakuohjeen hakeminen - tyhjä hakuohje"
  (let [hakemuskausi (test/next-hakemuskausi!)]
    (hk/find-hakuohje-sisalto (:vuosi hakemuskausi)) => nil))

(fact "Tallenna ja lataa määräräha"
  (let [maararaha {:vuosi vuosi :organisaatiolajitunnus "ELY" :maararaha 1M :ylijaama 1M}]

    (hk/save-maararaha! maararaha)
    (hk/find-maararaha vuosi "ELY") => (dissoc maararaha :vuosi :organisaatiolajitunnus)))

(fact "Tallenna hakuajat"
  (let [vuosi (:vuosi (test/next-hakemuskausi!))
        hakuaika {:alkupvm (time/today)
                  :loppupvm (time/plus (time/today) (time/days 1))}
        hakuajat [(assoc hakuaika :hakemustyyppitunnus "AH0")
                  (assoc hakuaika :hakemustyyppitunnus "MH1")
                  (assoc hakuaika :hakemustyyppitunnus "MH2")]]

    (hk/save-hakemuskauden-hakuajat! vuosi hakuajat)
    (:hakemukset  (find-hakemuskausi+ vuosi)) => (partial every? (c/eq :hakuaika hakuaika))))

(fact "Hakemuskausiyhteenvetohaku"
  (let [hakemuskausi (test/next-hakemuskausi!)
        vuosi (:vuosi hakemuskausi)
        hakemus1 {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid 1M}
        hakemus2 {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid 2M}
        id1 (h/add-hakemus! hakemus1)]

      (h/add-hakemus! hakemus2)
      (asha/with-asha-off (h/laheta-hakemus! id1))

      (find-hakemuskausi+ vuosi) =>
        {:vuosi      vuosi
         :tilatunnus "A"
         :hakuohje_contenttype nil
         :hakemukset #{{:hakemustyyppitunnus "AH0"
                       :hakemustilat #{{:hakemustilatunnus "K" :count 1M}, {:hakemustilatunnus "V" :count 1M}}
                       :hakuaika {:alkupvm (time/local-date (- vuosi 1) 9 1)
                                  :loppupvm (time/local-date (- vuosi 1) 12 15)}}

                       {:hakuaika {:alkupvm (time/local-date vuosi 7 1)
                                   :loppupvm (time/local-date vuosi 8 31)}, :hakemustilat #{}, :hakemustyyppitunnus "MH1"}

                       {:hakuaika {:alkupvm (time/local-date (+ vuosi 1) 1 1)
                                   :loppupvm (time/local-date (+ vuosi 1) 1 31)}, :hakemustilat #{}, :hakemustyyppitunnus "MH2"}}}))

