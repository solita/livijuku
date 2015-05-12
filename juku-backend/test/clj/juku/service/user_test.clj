(ns juku.service.user-test
  (:require [midje.sweet :refer :all]
            [juku.service.test :as test]
            [juku.service.user :as user]))

(fact "Käytäjän oikeudet"
  (test/with-user "juku_kasittelija" ["juku_kasittelija"]
    (user/has-privilege* "asdf") => false
    (user/has-privilege* "kasittely-hakemus") => true))
