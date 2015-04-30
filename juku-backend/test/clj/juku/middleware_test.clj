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
          user => (user/find-user uid)))

  (fact "Käyttäjän tiedon päivittäminen"
       (let [uid (str "tst" (rand-int 999999))
             user (m/save-user uid (:id tampere) {:oam-user-first-name "T" :oam-user-last-name "T"})
             updated-user (m/save-user uid (:id tampere) {:oam-user-first-name "A" :oam-user-last-name "A"})]

         updated-user => (user/find-user uid))))

(facts "User middleware"
  (fact "Uusi käyttäjä"
    (let [uid (str "tst" (rand-int 999999))
          user {:tunnus uid
                :organisaatioid (m/find-matching-organisaatio "liikennevirasto" nil)
                :etunimi "Päkä"
                :sukunimi "Pääkäyttäjä"
                :privileges (user/find-privileges ["juku_paakayttaja"])
                :jarjestelma false}

          request {:headers {"oam-remote-user"        uid
                             "oam-groups"             "juku_paakayttaja"
                             "oam-user-organization"  "liikennevirasto"
                             "oam-user-first-name"    "=?UTF-8?B?UMOka8Ok?="
                             "oam-user-last-name"     "=?UTF-8?B?UMOkw6Rrw6R5dHTDpGrDpA?="}}]
      ((m/wrap-user
         (fn [request] user/*current-user* => user))
        request)
      (dissoc user :privileges) => (user/find-user uid))))