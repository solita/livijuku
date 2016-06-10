(ns juku.service.test
  (:require [common.collection :as col]
            [common.core :as c]
            [slingshot.slingshot :as ss]
            [juku.service.hakemuskausi :as k]
            [juku.service.hakemus :as h]
            [juku.service.user :as user]
            [juku.middleware :as mw]
            [juku.db.database :refer [db with-transaction]]
            [juku.service.asiahallinta-mock :as asha]
            [yesql.core :as sql]
            [clj-time.core :as time]
            [clojure.string :as str]
            [common.collection :as coll])
  (:import (java.io ByteArrayInputStream)))

(sql/defqueries "juku/service/test.sql" {:connection db})

(defn find-next-notcreated-hakemuskausi []
  (int (+ (:next (first (select-max-vuosi-from-hakemuskausi))) 1)))

(defn init-hakemuskausi! [vuosi]
  (k/find-or-create-hakemuskausi! vuosi)
  (col/find-first (col/eq :vuosi vuosi) (k/find-hakemuskaudet+summary)))

(defn next-hakemuskausi! []
  (let [vuosi (find-next-notcreated-hakemuskausi)] (init-hakemuskausi! vuosi)))

(defn inputstream-from [txt] (ByteArrayInputStream. (.getBytes txt)))

(defn avaa-hakemuskausi! [vuosi]
  (asha/with-asha
    (k/save-hakuohje vuosi "test" "text/plain" (inputstream-from  "test"))
    (k/avaa-hakemuskausi! vuosi)))

(defn next-avattu-empty-hakemuskausi! []
  (let [hk (next-hakemuskausi!)
        vuosi (:vuosi hk)]
    (with-transaction

      (k/update-hakemuskausi-set-tila! {:vuosi vuosi :newtunnus "K" :expectedtunnus "A"})
      (k/save-hakuohje vuosi "test" "text/plain" (inputstream-from  "test"))
      (k/update-hakemuskausi-set-diaarinumero! {:vuosi vuosi
                                                :diaarinumero (str "test/" vuosi)}))
    hk))

(defn next-avattu-hakemuskausi! []
  (let [hk (next-hakemuskausi!)]
    (avaa-hakemuskausi! (:vuosi hk))
    hk))

(defn hakemus-summary [hakemuskausi hakemustyyppi]
  (first (filter (col/eq :hakemustyyppitunnus hakemustyyppi) (:hakemukset hakemuskausi))))

(defmacro with-user [uid rolenames & test]
  `(c/if-let* [roles# (user/find-roleids ~rolenames)
               user# (user/find-user ~uid)
               privileges# (user/find-privileges roles# (:organisaatioid user#))]
       (do
          (user/update-roles! ~uid roles#)
          (mw/with-user (assoc user# :privileges privileges#) ~@test))
       (ss/throw+ (str "Käyttäjällä " ~uid " ei ole voimassaolevaa käyttöoikeutta järjestelmään."))))

(defmacro with-hakija [& test] `(with-user "juku_hakija" ["juku_hakija"] ~@test))

(defmacro with-public-user [& test] `(mw/with-user mw/guest-user ~@test))

(defn before-now? [time]
  (time/before? time (time/now)))

(defn inputstream-from [txt] (ByteArrayInputStream. (.getBytes txt)))

(defn from-today [days]
  (time/plus (time/today) (time/days days)))

(defn before-today [days]
  (time/plus (time/today) (time/days days)))

(defn set-hakuaika-today [vuosi hakemustyyppitunnus]
  (k/save-hakemuskauden-hakuajat! vuosi [{:hakemustyyppitunnus hakemustyyppitunnus
                                           :alkupvm (time/today)
                                           :loppupvm (from-today 1)}]))

(defn laheta-all-ely-hakemukset [vuosi hakemustyyppitunnus]
  (with-hakija
    (with-transaction
      (doseq [hakemus (filter (coll/eq :hakemustilatunnus "K") (select-hakemukset-from-kausi {:vuosi vuosi :hakemustyyppitunnus (str/upper-case hakemustyyppitunnus)}))]
        (binding [user/*current-user* (assoc user/*current-user* :organisaatioid (:organisaatioid hakemus))]
          (h/laheta-hakemus! (:id hakemus)))))))

