(ns juku.service.hakemuskausi
  (:require [juku.service.organisaatio :as organisaatio]
            [juku.service.hakemus :as hakemus]
            [juku.service.asiahallinta :as asha]
            [juku.service.user :as user]
            [juku.service.organisaatio :as org]
            [juku.schema.hakemuskausi :as s]

            [juku.db.database :refer [db with-transaction]]
            [clojure.java.jdbc :as jdbc]
            [clojure.set :as set]
            [juku.db.sql :as dml]
            [juku.db.coerce :as coerce]
            [schema.coerce :as scoerce]
            [juku.db.yesql-patch :as sql]
            [common.collection :as col]
            [common.core :as c]
            [ring.util.http-response :as r]
            [clj-time.core :as time]
            [slingshot.slingshot :as ss])

  (:import (java.sql Blob)
           (java.io InputStream)))

(sql/defqueries "hakemuskausi.sql")

(def coerce-maararaha (scoerce/coercer s/Maararaha coerce/db-coercion-matcher))

(def coerce-hakemuskausi-summary (scoerce/coercer s/Hakemuskausi+Summary coerce/db-coercion-matcher))

(def constraint-errors
  {:hakemuskausi_pk {:http-response r/bad-request :message "Hakemuskausi on jo avattu vuodelle: {vuosi}"}})

(defn- oletushakemuskausi [vuosi] {:vuosi vuosi :tilatunnus "0" :hakuohje_contenttype nil})

(defn- coerce-vuosiluku->int [m] (update-in m [:vuosi] int))

(defn- find-all-hakemuskaudet+seuraava-kausi [new-hakemuskausi]
  (let [hakemuskaudet (map coerce-vuosiluku->int (select-all-hakemuskaudet))
        nextvuosi (+ (time/year (time/now)) 1)]
    (if (some (col/eq :vuosi nextvuosi) hakemuskaudet)
      hakemuskaudet
      (conj hakemuskaudet (new-hakemuskausi nextvuosi)))))

(defn find-hakemuskaudet+hakemukset []
  (let [init-hakemuskausi (fn [vuosi] (assoc (oletushakemuskausi vuosi) :hakemukset []))
        hakemuskaudet (find-all-hakemuskaudet+seuraava-kausi init-hakemuskausi)
        hakemukset (hakemus/find-all-hakemukset)]
    (col/assoc-left-join hakemuskaudet :hakemukset hakemukset :vuosi)))

(defn find-hakuohje-sisalto [vuosi]
  (if-let [ohje (first (select-hakuohje-sisalto {:vuosi vuosi}))]
    (update-in ohje [:sisalto] #(.getBinaryStream ^Blob %))))

(defn find-maararaha [vuosi organisaatiolajitunnus]
  (first (map coerce-maararaha (select-maararaha {:vuosi vuosi :organisaatiolajitunnus organisaatiolajitunnus}))))

(defn find-hakemuskausi [vuosi]
  (first (select-hakemuskausi vuosi)))

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

(defn hakuaika->hakemus-summary [hakuaika]
  {:hakemustyyppitunnus (:hakemustyyppitunnus hakuaika)
   :hakuaika {
              :alkupvm (:alkupvm hakuaika)
              :loppupvm (:loppupvm hakuaika)}
   :hakemustilat #{}})

(defn- oletus-hakemuskausi [vuosi]
  (assoc (oletushakemuskausi vuosi) :hakemukset
       #{(hakuaika->hakemus-summary(oletus-avustus-hakuaika vuosi))
         (hakuaika->hakemus-summary(oletus-maksatus-hakuaika1 vuosi))
         (hakuaika->hakemus-summary(oletus-maksatus-hakuaika2 vuosi))}))

(defn find-hakemuskaudet+summary []
  (let [hakemuskaudet (find-all-hakemuskaudet+seuraava-kausi oletus-hakemuskausi)
        hakemustyypit (map (comp coerce/row->object coerce-vuosiluku->int) (select-all-hakuajat))
        hakemustilat (map coerce-vuosiluku->int (count-hakemustilat-for-vuosi-hakemustyyppi))
        hakemustyypit+hakemustilat (col/assoc-left-join* hakemustyypit :hakemustilat hakemustilat #{} [:vuosi :hakemustyyppitunnus])]

    (map coerce-hakemuskausi-summary (col/assoc-left-join* hakemuskaudet :hakemukset hakemustyypit+hakemustilat [:vuosi]))))

(defn- insert-hakuaika! [hakuaika]
  (dml/insert db "hakuaika" (coerce/localdate->sql-date hakuaika) constraint-errors hakuaika))

(defn- insert-hakemuskauden-oletus-hakuajat! [vuosi]
  (let [assoc-vuosi (fn [m] (assoc m :vuosi vuosi))]
    (insert-hakuaika! (assoc-vuosi (oletus-avustus-hakuaika vuosi)))
    (insert-hakuaika! (assoc-vuosi (oletus-maksatus-hakuaika1 vuosi)))
    (insert-hakuaika! (assoc-vuosi (oletus-maksatus-hakuaika2 vuosi)))))

(defn init-hakemuskausi! [vuosi]
  (if (= (insert-hakemuskausi-if-not-exists! {:vuosi vuosi}) 1)
    (insert-hakemuskauden-oletus-hakuajat! vuosi) 0))

(defn- update-hakuaika! [hakuaika]
  (dml/update-where! db "hakuaika"
     (coerce/localdate->sql-date(dissoc hakuaika :vuosi :hakemustyyppitunnus))
     (select-keys hakuaika [:vuosi :hakemustyyppitunnus])))

(defn save-hakemuskauden-hakuajat! [vuosi hakuajat]
  (with-transaction
    (init-hakemuskausi! vuosi)
    (doseq [hakuaika hakuajat] (update-hakuaika! (assoc hakuaika :vuosi vuosi)))
    nil))

(defn save-hakuohje [^Integer vuosi nimi content-type ^InputStream hakuohje]
  (with-transaction
    (init-hakemuskausi! vuosi)
    (update-hakemuskausi-set-hakuohje! {:vuosi vuosi :nimi nimi :contenttype content-type :sisalto hakuohje})
    nil))

(defn avaa-hakemuskausi! [^Integer vuosi]
  (with-transaction

    (dml/assert-update (update-hakemuskausi-set-tila! {:vuosi vuosi :newtunnus "K" :expectedtunnus "A"})
       (if (empty? (select-hakemuskausi {:vuosi vuosi}))
         {:http-response r/not-found :message (str "Hakemuskautta ei ole olemassa vuodelle: " vuosi) :vuosi vuosi}
         {:http-response r/method-not-allowed :message (str "Hakemuskausi on jo avattu vuodelle: " vuosi) :vuosi vuosi}))

    (doseq [organisaatio (organisaatio/hakija-organisaatiot)]
      (let [hakemus (fn [hakemustyyppitunnus] {:vuosi vuosi :hakemustyyppitunnus hakemustyyppitunnus :organisaatioid (:id organisaatio)})]
        (hakemus/add-hakemus! (hakemus "AH0"))
        (hakemus/add-hakemus! (hakemus "MH1"))
        (hakemus/add-hakemus! (hakemus "MH2"))))

    ;; -- diaarioi hakemuskauden avaaminen --
    (c/if-let3! [hakuohje     (find-hakuohje-sisalto vuosi)
                              {:http-response r/not-found :message (str "Hakemuskaudella " vuosi "ei ole hakuohjetta.")}
                 organisaatio (org/find-organisaatio-of user/*current-user*)
                              {:http-response r/not-found :message (str "Käyttäjällä: '" (:tunnus user/*current-user*) "' ei ole organisaatiota")}]

      (update-hakemuskausi-set-diaarinumero! {:vuosi vuosi :diaarinumero
        (asha/avaa-hakemuskausi {:asianNimi             (str "Hakemuskausi " vuosi)
                                 :omistavaOrganisaatio  (:nimi organisaatio)
                                 :omistavaHenkilo       (user/user-fullname user/*current-user*)}
                                (set/rename-keys hakuohje {:sisalto :content :contenttype :mime-type}))})))
    nil)

(defn sulje-hakemuskausi! [^Integer vuosi]
  (if-let [hakemuskausi (find-hakemuskausi {:vuosi vuosi})]
    (with-transaction

      (dml/assert-update (update-hakemuskausi-set-tila! {:vuosi vuosi :newtunnus "S" :expectedtunnus "K"})
          {:http-response r/method-not-allowed :message (str "Hakemuskausi ei ole avattu vuodelle: " vuosi) :vuosi vuosi})

      (asha/sulje-hakemuskausi (:diaarinumero hakemuskausi)))

    (ss/throw+ {:http-response r/not-found :message (str "Hakemuskautta ei ole olemassa vuodelle: " vuosi) :vuosi vuosi})))


