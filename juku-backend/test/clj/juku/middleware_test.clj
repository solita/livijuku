(ns juku.middleware-test
  (:require [midje.sweet :refer :all]
            [juku.middleware :as m]
            [juku.service.user :as user]
            [common.collection :as coll]
            [juku.service.organisaatio :as org]))

(def tampere (coll/single-result-required (filter (coll/eq :nimi "Tampere") (org/organisaatiot)) :test {} "Testikaupunkia Tampere ei löytynyt"))

(facts "Find matching organization"

  (fact (m/find-matching-organisaatio "Tampereen kaupunki" nil) => (:id tampere) )
  (fact (m/find-matching-organisaatio "Tampereen \t kuntayhtymä" nil) => (:id tampere))
  (fact (m/find-matching-organisaatio "Tampere" nil) => nil))

(facts "User management"

  (fact "Uusi käyttäjä"
    (let [uid (str "tst" (rand-int 999999))
              user (m/save-user uid (:id tampere) {:oam-user-first-name "T" :oam-user-last-name "T"})]
          user => (dissoc (user/find-user uid) :jarjestelma)))

  (fact "Käyttäjän tiedon päivittäminen"
       (let [uid (str "tst" (rand-int 999999))
             user (m/save-user uid (:id tampere) {:oam-user-first-name "T" :oam-user-last-name "T"})
             updated-user (m/save-user uid (:id tampere) {:oam-user-first-name "A" :oam-user-last-name "A"})]

         updated-user => (user/find-user uid))))

#_(facts "User midleware"
  (fact "Uusi käyttäjä"
        (m/wrap-user )))