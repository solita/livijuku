(ns juku.service.test
  (:require [common.collection :as col]
            [common.core :as c]
            [slingshot.slingshot :as ss]
            [juku.service.hakemuskausi :as k]
            [juku.service.user :as user]
            [juku.user :as u]
            [juku.db.database :refer [db]]
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

(defn hakemus-summary [hakemuskausi hakemustyyppi]
  (first (filter (col/eq :hakemustyyppitunnus hakemustyyppi) (:hakemukset hakemuskausi))))

(defmacro with-user [uid roles & test]
  `(c/if-let* [privileges# (user/find-privileges ~roles)
               user# (user/find-user ~uid)]
       (u/with-user-id ~uid (user/with-user (assoc user# :privileges privileges#) ~@test))
       (ss/throw+ (str "Käyttäjällä " ~uid " ei ole voimassaolevaa käyttöoikeutta järjestelmään."))))

(defn before-now? [time]
  (time/before? time (time/now)))

(defn inputstream-from [txt] (ByteArrayInputStream. (.getBytes txt)))