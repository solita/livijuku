(ns juku.service.user-test
  (:require [midje.sweet :refer :all]
            [juku.service.test :as test]
            [juku.service.user :as user]
            [clj-time.core :as time]))

(fact "Käytäjän oikeudet"
  (test/with-user "juku_kasittelija" ["juku_kasittelija"]
    (user/has-privilege* :asdf) => false
    (user/has-privilege* :kasittely-hakemus) => true))

(fact "Käyttäjän tietojen päivitys"
  (test/with-user "juku_hakija" ["juku_hakija"]
    (let [assoc-roolit (fn [u] (assoc u :roolit ["Hakija"]))
          user-email-off {:sahkopostiviestit false}
          user-email-on {:sahkopostiviestit true}
          updated-user (dissoc (user/save-user! user-email-off) :privileges)]

      updated-user => (assoc-roolit (user/find-user (:tunnus user/*current-user*)))
      (user/save-user! user-email-on) => (assoc-roolit user/*current-user*))))

(fact "Käyttäjän omien tietojen haku"
  (test/with-user "juku_hakija" ["juku_hakija"]
    (let [user (user/current-user+updatekirjautumisaika!)]
      (dissoc user :kirjautumisaika) =>
        (assoc (dissoc user/*current-user* :kirjautumisaika) :roolit ["Hakija"])
      (:kirjautumisaika user) => (partial time/before? (:kirjautumisaika user/*current-user*)))))
