(ns juku.middleware-test
  (:require [midje.sweet :refer :all]
            [juku.middleware :as m]
            [juku.service.user :as user]
            [common.collection :as coll]
            [slingshot.slingshot :as ss]
            [juku.service.organisaatio :as org]))

(def tampere (coll/single-result-required! (filter (coll/eq :nimi "Tampere") (org/organisaatiot))
                                           {:message "Testikaupunkia Tampere ei löytynyt"}))

(defn create-headers [uid groups organization first-name last-name]
  {"iv-user"    uid
   "iv-groups" groups
   "o"          organization
   "givenname"  first-name
   "sn"         last-name})

(facts "Find matching organization"
  (m/find-matching-organisaatio "050834" nil) => (:id tampere)
  (m/find-matching-organisaatio "050834" "asdf") => (:id tampere)

  (m/find-matching-organisaatio "Liikennevirasto" nil) => 15M
  (m/find-matching-organisaatio "LiikenneVirasto" nil) => 15M
  (m/find-matching-organisaatio "Liikennevirasto \t 1234" nil) => 15M
  (m/find-matching-organisaatio "Liikenne" nil) => nil)

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
          organisaatioid (m/find-matching-organisaatio "liikennevirasto" nil)
          user {:tunnus uid
                :organisaatioid organisaatioid
                :etunimi "Päkä"
                :sukunimi "Pääkäyttäjä"
                :sahkoposti nil
                :sahkopostiviestit true
                :privileges (user/find-privileges ["PK"] organisaatioid)
                :jarjestelma false}

          request {:headers (create-headers
                              uid "juku_paakayttaja" "liikennevirasto"
                              "P%C3%A4k%C3%A4"
                              "P%C3%A4%C3%A4k%C3%A4ytt%C3%A4j%C3%A4")}]
      ((m/wrap-user
         (fn [request] (dissoc user/*current-user* :kirjautumisaika) => user))
        request)
      (dissoc (user/find-user uid) :kirjautumisaika) => (dissoc user :privileges)))

  (fact "Uusi käyttäjä - virheellinen ryhmätieto"
     (let [uid (str "tst" (rand-int 999999))
           request {:headers (create-headers uid "asdf" "liikennevirasto"
                                             "test" "test")}]

       (let [handler (fn [_] (ss/throw+ "käsittelijää ei pitäisi kutsua"))
             error ((m/wrap-user handler) request)]
         (:status error) => 403
         (:body error) => (str "Käyttäjällä " uid " ei ole yhtään juku-järjestelmän käyttäjäroolia - iv-groups: asdf"))
       (dissoc (user/find-user uid) :kirjautumisaika) => nil))


  (fact "Uusi käyttäjä - virheellinen organisaatio"
     (let [uid (str "tst" (rand-int 999999))
           request {:headers (create-headers uid "juku_paatoksentekija" "liikennevirast1" "test" "test")}]

       (let [handler (fn [_] (ss/throw+ "käsittelijää ei pitäisi kutsua"))
             error ((m/wrap-user handler) request)]
         (:status error) => 403
         (:body error) => (str "Käyttäjän " uid " organisaatiota: liikennevirast1 (osasto: ) ei tunnisteta."))
       (dissoc (user/find-user uid) :kirjautumisaika) => nil))

  (fact "Uusi käyttäjä - päällekkäinen otsikkotieto"
     (let [uid (str "tst" (rand-int 999999))

           request {:headers {"iv-user"        uid
                              "iv_user"        "asdf"
                              "oam-groups"             "asdf"
                              "oam-user-organization"  "liikennevirasto"
                              "oam-user-first-name"    "test"
                              "oam-user-last-name"     "test"}}]

       (let [handler (fn [_] (ss/throw+ "käsittelijää ei pitäisi kutsua"))
             error ((m/wrap-user handler) request)]
         (:status error) => 400
         (:body error) => "Pyynnön otsikkotiedossa on päällekkäisiä otsikoita.")
       (dissoc (user/find-user uid) :kirjautumisaika) => nil)))