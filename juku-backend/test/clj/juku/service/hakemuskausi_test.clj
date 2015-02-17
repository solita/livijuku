(ns juku.service.hakemuskausi-test
  (:require [midje.sweet :refer :all]
            [juku.service.hakemuskausi :as h]
            [juku.service.test :as t])
  (:import (java.io ByteArrayInputStream)))

(fact "Uuden hakuohjeen tallentaminen ja hakeminen"
  (let [vuosi 99
        hakuohje {:vuosi vuosi :nimi "test" :contenttype "text/plain"}]

    (h/save-hakuohje vuosi "test" "text/plain" (ByteArrayInputStream. (.getBytes "test")))
    (slurp (:sisalto (h/find-hakuohje-sisalto vuosi))) => "test"))



(fact "Avaa hakemuskausi"
  (let [vuosi (t/find-next-notcreated-hakemuskausi)
        hakuohje {:vuosi vuosi :nimi "test" :contenttype "text/plain"}]

    (h/save-hakuohje vuosi "test" "text/plain" (ByteArrayInputStream. (.getBytes "test")))
    (h/avaa-hakemuskausi! vuosi)))


(fact "Tallenna ja lataa määräräha"
  (let [vuosi 99
        maararaha {:vuosi vuosi :organisaatiolajitunnus "ELY" :maararaha 1M :ylijaama 1M}]

    (h/save-maararaha! maararaha)
    (h/find-maararaha vuosi "ELY") => (dissoc maararaha :vuosi :organisaatiolajitunnus)))