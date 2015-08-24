(ns juku.service.test
  (:require [common.collection :as col]
            [common.core :as c]
            [slingshot.slingshot :as ss]
            [juku.service.hakemuskausi :as k]
            [juku.service.user :as user]
            [juku.user :as u]
            [juku.db.database :refer [db with-transaction]]
            [juku.service.asiahallinta-mock :as asha]
            [yesql.core :as sql]
            [clj-time.core :as time])
  (:import (java.io ByteArrayInputStream)))

(sql/defqueries "juku/service/test.sql" {:connection db})

(defn find-next-notcreated-hakemuskausi []
  (int (+ (:next (first (select-max-vuosi-from-hakemuskausi))) 1)))

(defn init-hakemuskausi! [vuosi]
  (k/init-hakemuskausi! vuosi)
  (first (filter (col/eq :vuosi vuosi) (k/find-hakemuskaudet+summary))))

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

(defn hakemus-summary [hakemuskausi hakemustyyppi]
  (first (filter (col/eq :hakemustyyppitunnus hakemustyyppi) (:hakemukset hakemuskausi))))

(defmacro with-user [uid roles & test]
  `(c/if-let* [privileges# (user/find-privileges ~roles)
               user# (user/find-user ~uid)]
       (let [user+roles# (if (user/update-roles! ~uid ~roles) (user/find-user ~uid) user#)]
          (u/with-user-id ~uid (user/with-user (assoc user+roles# :privileges privileges#) ~@test)))
       (ss/throw+ (str "Käyttäjällä " ~uid " ei ole voimassaolevaa käyttöoikeutta järjestelmään."))))

(defn before-now? [time]
  (time/before? time (time/now)))

(defn inputstream-from [txt] (ByteArrayInputStream. (.getBytes txt)))

(defn from-today [days]
  (time/plus (time/today) (time/days days)))

(defn before-today [days]
  (time/plus (time/today) (time/days days)))

