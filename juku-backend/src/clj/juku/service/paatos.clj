(ns juku.service.paatos
  (:require [juku.db.yesql-patch :as sql]
            [clojure.string :as str]
            [common.string :as xstr]
            [juku.db.database :refer [db with-transaction]]
            [juku.db.coerce :as coerce]
            [juku.service.hakemus :as h]
            [juku.service.avustuskohde :as ak]
            [juku.service.pdf :as pdf]
            [juku.service.organisaatio :as o]
            [schema.coerce :as scoerce]
            [juku.schema.paatos :as s]
            [common.collection :as col]
            [common.core :as c]
            [clojure.java.io :as io]
            [clj-time.core :as time])
  (:import (org.joda.time LocalDate)))

(sql/defqueries "paatos.sql")

(def coerce-paatos (scoerce/coercer s/Paatos coerce/db-coercion-matcher))

(defn find-current-paatos [hakemusid]
  (first (map coerce-paatos (select-current-paatos {:hakemusid hakemusid}))))

(defn find-paatos [hakemusid paatosnumero]
  (first (map coerce-paatos (select-paatos {:hakemusid hakemusid :paatosnumero paatosnumero}))))

(defn new-paatos! [paatos] (insert-paatos! paatos))

(defn- assert-update [updateamount hakemusid]
  (assert (< updateamount 2) (str "Tietokannan tiedot ovat virheelliset. Hakemuksella " hakemusid " on kaksi avointa päätöstä.")))

(defn save-paatos! [paatos]
  (let [updated (update-paatos! paatos)]
    (assert-update updated (:hakemusid paatos))
    (if (== updated 0) (new-paatos! paatos))
    nil))

(defn hyvaksy-paatos! [hakemusid]
  (with-transaction
     (let [updated (update-paatos-hyvaksytty! {:hakemusid hakemusid})]
        (assert-update updated hakemusid)
        (cond
           (== updated 1) (h/update-hakemustila! {:hakemusid hakemusid :hakemustilatunnus "P"})
           (== updated 0) (col/not-found! ::paatos-not-found {:hakemusid hakemusid} (str "Hakemuksella " hakemusid " ei ole avointa päätöstä")))))
  nil)

(defn paatos-pdf [hakemusid, paatosnumero]
  (let [paatos (find-paatos hakemusid paatosnumero)
        paatospvm-txt (.toString ^LocalDate (or (:voimaantuloaika paatos) (time/today)) "d.M.y")
        hakemus (h/get-hakemus-by-id hakemusid)
        organisaatio (o/find-organisaatio (:organisaatioid hakemus))
        avustuskohteet (ak/find-avustuskohteet-by-hakemusid hakemusid)

        total-haettavaavustus (reduce + 0 (map :haettavaavustus avustuskohteet))
        total-omarahoitus (reduce + 0 (map :omarahoitus avustuskohteet))

        template (slurp (io/reader (io/resource "pdf-sisalto/templates/paatos.txt")))]

      (pdf/muodosta-pdf
          {:otsikko {:teksti "Valtionavustuspäätös" :paivays paatospvm-txt :diaarinumero (:diaarinumero hakemus)}
           :teksti (xstr/interpolate template
                         {:organisaatio-nimi (:nimi organisaatio)
                          :organisaatiolaji-pl-gen (h/organisaatiolaji->plural-genetive (:lajitunnus organisaatio))
                          :paatosspvm paatospvm-txt
                          :vuosi (:vuosi hakemus)
                          :avustuskohteet (ak/avustuskohteet-section avustuskohteet)
                          :haettuavustus total-haettavaavustus
                          :selite (c/maybe-nil #(str % "\n\n\t") "" (:selite paatos))
                          :omarahoitus total-omarahoitus
                          :myonnettyavustus (:myonnettyavustus paatos)})
           :footer "Footer"})))

(defn peruuta-paatos! [hakemusid]
  (with-transaction
    (let [updated (update-paatos-hylatty! {:hakemusid hakemusid})]
      (assert-update updated hakemusid)
      (cond
        (== updated 1) (h/update-hakemustila! {:hakemusid hakemusid :hakemustilatunnus "T"})
        (== updated 0) (col/not-found! ::paatos-not-found {:hakemusid hakemusid} (str "Hakemuksella " hakemusid " ei ole voimassaolevaa päätöstä")))))
  nil)