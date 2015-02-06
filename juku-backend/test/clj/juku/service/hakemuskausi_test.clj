(ns juku.service.hakemuskausi-test
  (:require [midje.sweet :refer :all]
            [clj-time.core :as t]
            [clojure.java.io :as io]
            [juku.service.hakemuskausi :as h])
  (:import (java.io ByteArrayInputStream)))

(fact "Uuden hakuohjeen tallentaminen ja hakeminen"
  (let [vuosi 99
        hakuohje {:vuosi vuosi :nimi "test" :contenttype "text/plain"}]

    (h/save-hakuohje vuosi "test" "text/plain" (ByteArrayInputStream. (.getBytes "test")))
    (slurp (:sisalto (h/find-hakuohje-sisalto vuosi))) => "test"))


