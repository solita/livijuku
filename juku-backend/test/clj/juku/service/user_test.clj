(ns juku.service.user-test
  (:require [midje.sweet :refer :all]
            [juku.service.test :as test]
            [juku.service.user :as user]
            [clojure.tools.logging :as log]
            [clj-time.core :as time]))

(fact "Käytäjän oikeudet"
  (test/with-user "juku_kasittelija" ["juku_kasittelija"]
    (user/has-privilege* :asdf) => false
    (user/has-privilege* :kasittely-hakemus) => true))

(defn assoc-roolit [u] (assoc u :roolit ["Hakija"]))

(fact "Käyttäjän tietojen päivitys"
  (test/with-user "juku_hakija" ["juku_hakija"]
    (let [user-email-off {:sahkopostiviestit false}
          user-email-on {:sahkopostiviestit true}
          updated-user (dissoc (user/save-user! user-email-off) :privileges)]

      updated-user => (assoc-roolit (user/find-user (:tunnus user/*current-user*)))
      (user/save-user! user-email-on) => (assoc-roolit user/*current-user*))))

(fact "Käyttäjän omien tietojen haku"
  (test/with-user "juku_hakija" ["juku_hakija"]
    (let [user (user/current-user+updatekirjautumisaika!)]
      (dissoc user :kirjautumisaika) => (assoc-roolit (dissoc user/*current-user* :kirjautumisaika))
      (log/info (:kirjautumisaika user) " > " (:kirjautumisaika user/*current-user*))
      (:kirjautumisaika user) => (partial time/before? (:kirjautumisaika user/*current-user*)))))
