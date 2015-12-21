(ns juku.service.hakemuskausi
  (:require [juku.service.organisaatio :as organisaatio]
            [juku.service.hakemus-core :as hakemus]
            [juku.service.asiahallinta :as asha]
            [juku.service.user :as user]
            [juku.service.organisaatio :as org]
            [juku.schema.hakemuskausi :as s]

            [juku.db.database :refer [db with-transaction]]
            [clojure.string :as str]
            [juku.db.sql :as dml]
            [juku.db.coerce :as coerce]
            [schema.coerce :as scoerce]
            [juku.db.yesql-patch :as sql]
            [juku.settings :refer [settings asiahallinta-on?]]
            [common.collection :as col]
            [common.core :as c]
            [common.map :as m]
            [ring.util.http-response :as r]
            [clj-time.core :as time]
            [slingshot.slingshot :as ss]
            [common.collection :as coll]
            [common.string :as strx])

  (:import (java.sql Blob)
           (java.io InputStream)))

(sql/defqueries "hakemuskausi.sql")

(def coerce-maararaha (scoerce/coercer s/Maararaha coerce/db-coercion-matcher))

(def coerce-hakemuskausi-summary (scoerce/coercer s/Hakemuskausi+Summary coerce/db-coercion-matcher))

(def coerce-hakuaika (scoerce/coercer s/Hakuaika+ coerce/db-coercion-matcher))

(def constraint-errors
  {:hakemuskausi_pk {:http-response r/bad-request :message "Hakemuskausi on jo avattu vuodelle: {vuosi}"}})

(defn- oletushakemuskausi [vuosi] {:vuosi vuosi :tilatunnus "0" :hakuohje_contenttype nil})

(defn- coerce-vuosiluku->int [m] (update-in m [:vuosi] int))

(defn nextvuosi
  ([] (nextvuosi (time/now)))
  ([now] (let [current-vuosi (time/year now)]
            (if (time/after? now (time/from-time-zone (time/date-time current-vuosi 7 1)
                                                      (time/time-zone-for-id "Europe/Helsinki")))
              (inc current-vuosi)
              current-vuosi))))

(defn- find-all-hakemuskaudet+seuraava-kausi [new-hakemuskausi]
  (let [hakemuskaudet (map coerce-vuosiluku->int (select-all-hakemuskaudet))
        nextvuosi (nextvuosi)]
    (if (some (col/eq :vuosi nextvuosi) hakemuskaudet)
      hakemuskaudet
      (conj hakemuskaudet (new-hakemuskausi nextvuosi)))))

(defn find-hakemuskaudet+hakemukset []
  (let [init-hakemuskausi (fn [vuosi] (assoc (oletushakemuskausi vuosi) :hakemukset []))
        hakemuskaudet (find-all-hakemuskaudet+seuraava-kausi init-hakemuskausi)
        hakemukset (hakemus/find-all-hakemukset)]
    (col/assoc-join hakemuskaudet :hakemukset hakemukset [:vuosi])))

(defn find-kayttajan-hakemuskaudet+hakemukset []
  (let [hakemuskaudet (filter (comp #{"K" "S"} :tilatunnus) (map coerce-vuosiluku->int (select-all-hakemuskaudet)))
        hakemukset (hakemus/find-kayttajan-hakemukset)]
    (col/assoc-join hakemuskaudet :hakemukset hakemukset [:vuosi])))

(defn- find-liite-sisalto [select vuosi]
  (if-let [ohje (first (select {:vuosi vuosi}))]
    (update-in ohje [:sisalto] #(.getBinaryStream ^Blob %))))

(defn find-hakuohje-sisalto [vuosi] (find-liite-sisalto select-hakuohje-sisalto vuosi))

(defn find-ely-hakuohje-sisalto [vuosi] (find-liite-sisalto select-ely-hakuohje-sisalto vuosi))

(defn find-maararaha [vuosi organisaatiolajitunnus]
  (first (map coerce-maararaha (select-maararaha {:vuosi vuosi :organisaatiolajitunnus organisaatiolajitunnus}))))

(defn- update-maararaha! [maararaha]
  (dml/update-where! db "maararaha"
                     (dissoc maararaha :vuosi :organisaatiolajitunnus)
                     (select-keys maararaha [:vuosi :organisaatiolajitunnus])))

(defn- insert-maararaha! [maararaha]
  (dml/insert db "maararaha" maararaha constraint-errors maararaha))

(defn save-maararaha! [maararaha]
  (if (= (update-maararaha! maararaha) 0) (insert-maararaha! maararaha)))

(defn- oletus-avustus-hakuaika [vuosi] {
     :hakemustyyppitunnus "AH0"
     :alkupvm (time/local-date (- vuosi 1) 9 1)
     :loppupvm (time/local-date (- vuosi 1) 12 15)})

(defn- oletus-maksatus-hakuaika1 [vuosi] {
     :hakemustyyppitunnus "MH1"
     :alkupvm (time/local-date vuosi 7 1)
     :loppupvm (time/local-date vuosi 8 31)})

(defn- oletus-maksatus-hakuaika2 [vuosi] {
       :hakemustyyppitunnus "MH2"
       :alkupvm (time/local-date (+ vuosi 1) 1 1)
       :loppupvm (time/local-date (+ vuosi 1) 1 31)})

(defn- oletus-ely-hakuaika [vuosi] {
      :hakemustyyppitunnus "ELY"
      :alkupvm (time/local-date (- vuosi 1) 9 1)
      :loppupvm (time/local-date (- vuosi 1) 10 31)})

(defn hakuaika->hakemus-summary [hakuaika]
  {:hakemustyyppitunnus (:hakemustyyppitunnus hakuaika)
   :hakuaika {
              :alkupvm (:alkupvm hakuaika)
              :loppupvm (:loppupvm hakuaika)}
   :hakemustilat []})

(defn- oletus-hakemuskausi [vuosi]
  (assoc (oletushakemuskausi vuosi) :hakemukset
       [(hakuaika->hakemus-summary(oletus-avustus-hakuaika vuosi))
        (hakuaika->hakemus-summary(oletus-maksatus-hakuaika1 vuosi))
        (hakuaika->hakemus-summary(oletus-maksatus-hakuaika2 vuosi))
        (hakuaika->hakemus-summary(oletus-ely-hakuaika vuosi))]))

(defn find-hakemuskaudet+summary []
  (let [hakemuskaudet (find-all-hakemuskaudet+seuraava-kausi oletus-hakemuskausi)
        hakemustyypit (map (comp coerce/row->object coerce-vuosiluku->int) (select-all-hakuajat))
        hakemustilat (map coerce-vuosiluku->int (count-hakemustilat-for-vuosi-hakemustyyppi))
        hakemustyypit+hakemustilat (col/assoc-join hakemustyypit :hakemustilat hakemustilat [:vuosi :hakemustyyppitunnus]
                                                   col/dissoc-join-keys)]

    (map coerce-hakemuskausi-summary (col/assoc-join-if-not-nil
                                       hakemuskaudet :hakemukset hakemustyypit+hakemustilat [:vuosi]
                                       col/dissoc-join-keys))))

(defn- insert-hakuaika! [hakuaika]
  (dml/insert db "hakuaika" hakuaika constraint-errors hakuaika))

(defn- insert-hakemuskauden-oletus-hakuajat! [vuosi]
  (let [assoc-vuosi (fn [m] (assoc m :vuosi vuosi))]
    (insert-hakuaika! (assoc-vuosi (oletus-avustus-hakuaika vuosi)))
    (insert-hakuaika! (assoc-vuosi (oletus-maksatus-hakuaika1 vuosi)))
    (insert-hakuaika! (assoc-vuosi (oletus-maksatus-hakuaika2 vuosi)))
    (insert-hakuaika! (assoc-vuosi (oletus-ely-hakuaika vuosi)))))

(defn find-or-create-hakemuskausi! [vuosi]
  (if-let [hakemuskausi (hakemus/find-hakemuskausi {:vuosi vuosi})]
    hakemuskausi
    (if (> vuosi (nextvuosi))
      (ss/throw+  {:http-response r/bad-request
                   :message (str "Hakemuskautta " vuosi
                                 " ei ole vielä mahdollista luoda. Uusin sallittu hakemuskausi on " (nextvuosi))})
      (with-transaction
        (dml/insert db "hakemuskausi" {:vuosi vuosi} constraint-errors {:vuosi vuosi})
        (insert-hakemuskauden-oletus-hakuajat! vuosi)))))

(defn- update-hakuaika! [hakuaika]
  (dml/update-where! db "hakuaika"
     (dissoc hakuaika :vuosi :hakemustyyppitunnus)
     (select-keys hakuaika [:vuosi :hakemustyyppitunnus])))

(defn save-hakemuskauden-hakuajat! [vuosi hakuajat]
  (with-transaction
    (find-or-create-hakemuskausi! vuosi)
    (doseq [hakuaika hakuajat] (update-hakuaika! (assoc hakuaika :vuosi vuosi)))
    nil))

(defn- assert-hakuohje-editable! [vuosi hakemuskausi]
  (when (= (:tilatunnus hakemuskausi) "S")
    (ss/throw+  {:http-response r/conflict
                 :message (str "Hakemuskausi " vuosi " on suljettu. Hakuohjetta ei voi enää päivittää.")})))

(defn save-hakuohje [^Integer vuosi nimi content-type ^InputStream hakuohje]
  (with-transaction
    (let [hakemuskausi (find-or-create-hakemuskausi! vuosi)]
      (assert-hakuohje-editable! vuosi hakemuskausi)

      (update-hakemuskausi-set-hakuohje! {:vuosi vuosi :nimi nimi :contenttype content-type :sisalto hakuohje})

      (when (and (= (:tilatunnus hakemuskausi) "K") (:diaarinumero hakemuskausi))
        (asha/update-hakuohje (:diaarinumero hakemuskausi)
                              {:kasittelija (user/user-fullname user/*current-user*)}
                              (find-hakuohje-sisalto vuosi)))
      nil)))

(defn save-ely-hakuohje [^Integer vuosi nimi content-type ^InputStream hakuohje]
  (with-transaction
    (let [hakemuskausi (find-or-create-hakemuskausi! vuosi)]
      (assert-hakuohje-editable! vuosi hakemuskausi)

      (update-hakemuskausi-set-ely-hakuohje! {:vuosi vuosi :nimi nimi :contenttype content-type :sisalto hakuohje})
      nil)))

(defn find-hakuajat [vuosi]
  (m/map-values (comp coerce-hakuaika first)
                (group-by (comp keyword str/lower-case :hakemustyyppitunnus)
                          (select-hakuajat-by-vuosi {:vuosi vuosi}))))

(defn avaa-hakemuskausi! [^Integer vuosi]
  (with-transaction

    (dml/assert-update (update-hakemuskausi-set-tila! {:vuosi vuosi :newtunnus "K" :expectedtunnus "A"})
       (if (empty? (hakemus/select-hakemuskausi {:vuosi vuosi}))
         {:http-response r/not-found :message (str "Hakemuskautta ei ole olemassa vuodelle: " vuosi) :vuosi vuosi}
         {:http-response r/conflict :message (str "Hakemuskausi on jo avattu vuodelle: " vuosi) :vuosi vuosi}))


    (insert-hakemukset-for-kausi! {:vuosi vuosi})

    (insert-avustuskohteet-for-kausi! {:vuosi vuosi})

    (insert-maararahatarpeet-for-kausi! {:vuosi vuosi})

    ;; -- diaarioi hakemuskauden avaaminen --
    (when (asiahallinta-on?)
      (c/if-let3! [hakuohje (find-hakuohje-sisalto vuosi)
                    {:http-response r/not-found :message (str "Hakemuskaudella " vuosi "ei ole hakuohjetta.")}
                   diaarinumero (strx/trim (asha/avaa-hakemuskausi {:asianNimi (str "Hakemuskausi " vuosi)} hakuohje))
                    {:type :arkistointi :message (str "Asiahallintajärjestelmä ei palauttanut diaarinumeroa kaudelle: " vuosi)}]

        (update-hakemuskausi-set-diaarinumero! {:vuosi vuosi :diaarinumero diaarinumero}))))
    nil)

(defn sulje-hakemuskausi! [^Integer vuosi]
  (if-let [hakemuskausi (hakemus/find-hakemuskausi {:vuosi vuosi})]
    (with-transaction

      (dml/assert-update (update-hakemuskausi-set-tila! {:vuosi vuosi :newtunnus "S" :expectedtunnus "K"})
          {:http-response r/conflict :message (str "Hakemuskausi ei ole avattu vuodelle: " vuosi) :vuosi vuosi})

      (sulje-kaikki-hakemuskauden-hakemukset! {:vuosi vuosi})

      (if (:diaarinumero hakemuskausi) (asha/sulje-hakemuskausi (:diaarinumero hakemuskausi))))

    (ss/throw+ {:http-response r/not-found :message (str "Hakemuskautta ei ole olemassa vuodelle: " vuosi) :vuosi vuosi})))

