(ns juku.service.user-test
  (:require [midje.sweet :refer :all]
            [juku.service.test :as test]
            [juku.service.user :as user]))

(fact "Käytäjän oikeudet"
  (test/with-user "juku_kasittelija" ["juku_kasittelija"]
    (user/has-privilege* :asdf) => false
    (user/has-privilege* :kasittely-hakemus) => true))

(fact "Käyttäjän tietojen päivitys"
  (test/with-user "juku_hakija" ["juku_hakija"]
    (let [user-email-off {:sahkopostiviestit false}
          user-email-on {:sahkopostiviestit true}
          updated-user (dissoc (user/save-user! user-email-off) :privileges :roolit)]

      updated-user => (user/find-user (:tunnus user/*current-user*))
      (user/save-user! user-email-on) => user/*current-user*)))
