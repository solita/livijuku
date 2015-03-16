(ns juku.service.hakemuskausi-test
  (:require [midje.sweet :refer :all]
            [juku.service.hakemuskausi :as hk]
            [juku.service.hakemus :as h]
            [common.collection :as c]
            [juku.service.test :as test]
            [clj-time.core :as time])
  (:import (java.io ByteArrayInputStream)))

(def hakemuskausi (test/next-hakemuskausi!))
(def vuosi (:vuosi hakemuskausi))

(fact "Avaa hakemuskausi"
  (let [vuosi (test/find-next-notcreated-hakemuskausi)]
    (hk/avaa-hakemuskausi! vuosi)))

(fact "Uuden hakuohjeen tallentaminen ja hakeminen"
  (let [hakuohje {:vuosi vuosi :nimi "test" :contenttype "text/plain"}]

    (hk/save-hakuohje vuosi "test" "text/plain" (ByteArrayInputStream. (.getBytes "test")))
    (slurp (:sisalto (hk/find-hakuohje-sisalto vuosi))) => "test"))


(fact "Tallenna ja lataa määräräha"
  (let [maararaha {:vuosi vuosi :organisaatiolajitunnus "ELY" :maararaha 1M :ylijaama 1M}]

    (hk/save-maararaha! maararaha)
    (hk/find-maararaha vuosi "ELY") => (dissoc maararaha :vuosi :organisaatiolajitunnus)))

(fact "Hakemuskausiyhteenvetohaku"
  (let [hakemuskausi (test/next-hakemuskausi!)
        vuosi (:vuosi hakemuskausi)
        hakemus1 {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid 1M}
        hakemus2 {:vuosi vuosi :hakemustyyppitunnus "AH0" :organisaatioid 2M}
        id1 (h/add-hakemus! hakemus1)]

      (h/add-hakemus! hakemus2)
      (h/laheta-hakemus! id1)

      (c/find-first (c/eq :vuosi vuosi) (hk/find-hakemuskaudet+summary)) =>
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
                                   :loppupvm (time/local-date (+ vuosi 1) 1 31)}, :hakemustilat #{}, :hakemustyyppitunnus "MH2"}}}
    ))

