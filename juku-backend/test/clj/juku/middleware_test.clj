(ns juku.middleware-test
  (:require [midje.sweet :refer :all]
            [juku.middleware :as m]
            [juku.service.user :as user]
            [common.collection :as coll]
            [slingshot.slingshot :as ss]
            [juku.service.organisaatio :as org]))

(def tampere (coll/single-result-required! (filter (coll/eq :nimi "Tampere") (org/organisaatiot))
                                           {:message "Testikaupunkia Tampere ei löytynyt"}))

(facts "Find matching organization"

  (fact (m/find-matching-organisaatio "Tampereen kaupunki" nil) => (:id tampere) )
  (fact (m/find-matching-organisaatio "Tampereen \t kuntayhtymä" nil) => (:id tampere))
  (fact (m/find-matching-organisaatio "Tampere" nil) => nil))

(facts "User management"

  (fact "Uusi käyttäjä"
    (let [uid (str "tst" (rand-int 999999))
          user (m/save-user uid (:id tampere) ["juku_hakija"] {:oam-user-first-name "T" :oam-user-last-name "T"})]

          user => (user/find-user uid)))

  (fact "Käyttäjän tiedon päivittäminen"
       (let [uid (str "tst" (rand-int 999999))
             user (m/save-user uid (:id tampere) ["juku_hakija"] {:oam-user-first-name "T" :oam-user-last-name "T"})
             updated-user (m/save-user uid (:id tampere) ["juku_hakija"] {:oam-user-first-name "A" :oam-user-last-name "A"})]

         updated-user => (user/find-user uid))))

(facts "User middleware"
  (fact "Uusi käyttäjä"
    (let [uid (str "tst" (rand-int 999999))
          user {:tunnus uid
                :organisaatioid (m/find-matching-organisaatio "liikennevirasto" nil)
                :etunimi "Päkä"
                :sukunimi "Pääkäyttäjä"
                :sahkoposti nil
                :sahkopostiviestit true
                :privileges (user/find-privileges ["PK"])
                :jarjestelma false}

          request {:headers {"oam-remote-user"        uid
                             "oam-groups"             "juku_paakayttaja"
                             "oam-user-organization"  "liikennevirasto"
                             "oam-user-first-name"    "=?UTF-8?B?UMOka8Ok?="
                             "oam-user-last-name"     "=?UTF-8?B?UMOkw6Rrw6R5dHTDpGrDpA?="}}]
      ((m/wrap-user
         (fn [request] (dissoc user/*current-user* :kirjautumisaika) => user))
        request)
      (dissoc (user/find-user uid) :kirjautumisaika) => (dissoc user :privileges)))

  (fact "Uusi käyttäjä - virheellinen ryhmätieto"
     (let [uid (str "tst" (rand-int 999999))
           request {:headers {"oam-remote-user"        uid
                              "oam-groups"             "asdf"
                              "oam-user-organization"  "liikennevirasto"
                              "oam-user-first-name"    "test"
                              "oam-user-last-name"     "test"}}]

       (let [handler (fn [_] (ss/throw+ "käsittelijää ei pitäisi kutsua"))
             error ((m/wrap-user handler) request)]
         (:status error) => 403
         (:body error) => (str "Käyttäjällä " uid " ei ole yhtään juku-järjestelmän käyttäjäroolia - oam-groups: asdf"))
       (dissoc (user/find-user uid) :kirjautumisaika) => nil))


  (fact "Uusi käyttäjä - virheellinen organisaatio"
     (let [uid (str "tst" (rand-int 999999))
           request {:headers {"oam-remote-user"        uid
                              "oam-groups"             "juku_paatoksentekija"
                              "oam-user-organization"  "liikennevirast1"
                              "oam-user-first-name"    "test"
                              "oam-user-last-name"     "test"}}]

       (let [handler (fn [_] (ss/throw+ "käsittelijää ei pitäisi kutsua"))
             error ((m/wrap-user handler) request)]
         (:status error) => 403
         (:body error) => (str "Käyttäjän " uid " organisaatiota: liikennevirast1 (osasto: ) ei tunnisteta."))
       (dissoc (user/find-user uid) :kirjautumisaika) => nil))

  (fact "Uusi käyttäjä - päällekkäinen otsikkotieto"
     (let [uid (str "tst" (rand-int 999999))

           request {:headers {"oam-remote-user"        uid
                              "oam_remote-user"        "asdf"
                              "oam-groups"             "asdf"
                              "oam-user-organization"  "liikennevirasto"
                              "oam-user-first-name"    "test"
                              "oam-user-last-name"     "test"}}]

       (let [handler (fn [_] (ss/throw+ "käsittelijää ei pitäisi kutsua"))
             error ((m/wrap-user handler) request)]
         (:status error) => 400
         (:body error) => "Pyynnön otsikkotiedossa on päällekkäisiä otsikoita.")
       (dissoc (user/find-user uid) :kirjautumisaika) => nil)))