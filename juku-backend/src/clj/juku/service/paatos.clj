(ns juku.service.paatos
  (:require [yesql.core :as sql]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as str]
            [common.string :as xstr]
            [juku.db.database :refer [db]]
            [juku.db.coerce :as coerce]
            [juku.service.hakemus :as h]
            [juku.service.pdf :as pdf]
            [juku.service.organisaatio :as o]
            [schema.coerce :as scoerce]
            [juku.schema.paatos :as s]
            [common.collection :as c]
            [clojure.java.io :as io]
            [clj-time.core :as time])
  (:import (java.time LocalDate)))

(sql/defqueries "paatos.sql" {:connection db})

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
  (jdbc/with-db-transaction [db-spec db]
     (let [updated (update-paatos-hyvaksytty! {:hakemusid hakemusid})]
        (assert-update updated hakemusid)
        (cond
           (== updated 1) (h/update-hakemustila! {:hakemusid hakemusid :hakemustilatunnus "P"})
           (== updated 0) (c/not-found! ::paatos-not-found {:hakemusid hakemusid} (str "Hakemuksella " hakemusid " ei ole avointa päätöstä")))))
  nil)

(defn paatos-pdf [hakemusid, paatosnumero]
  (let [paatos (find-paatos hakemusid paatosnumero)
        paatospvm-txt (.toString ^LocalDate (or (:voimaantuloaika paatos) (time/today)) "d.M.y")
        hakemus (h/get-hakemus-by-id hakemusid)
        organisaatio (c/find-first (c/eq :id (:organisaatioid hakemus)) (o/organisaatiot))
        avustuskohteet (h/find-avustuskohteet-by-hakemusid hakemusid)

        total-haettavaavustus (reduce + 0 (map :haettavaavustus avustuskohteet))
        total-omarahoitus (reduce + 0 (map :omarahoitus avustuskohteet))

        avustuskohde-template "\t{avustuskohdelajitunnus},\t{haettavaavustus} euroa"
        avustuskohteet-section (str/join "\n" (map (partial xstr/interpolate avustuskohde-template)
                                                   (filter (c/predicate > :haettavaavustus 0) avustuskohteet)))

        template (slurp (io/reader (io/resource "pdf-sisalto/templates/paatos.txt")))]

      (pdf/muodosta-pdf
          {:otsikko {:teksti "Valtionavustuspäätös" :paivays paatospvm-txt :diaarinumero (:diaarinumero hakemus)}
           :teksti (xstr/interpolate template
                         {:organisaatio-nimi (:nimi organisaatio)
                          :paatosspvm paatospvm-txt
                          :vuosi (:vuosi hakemus)
                          :avustuskohteet avustuskohteet-section
                          :haettuavustus total-haettavaavustus
                          :omarahoitus total-omarahoitus
                          :myonnettyavustus (:myonnettyavustus paatos)})
           :footer "Footer"})))