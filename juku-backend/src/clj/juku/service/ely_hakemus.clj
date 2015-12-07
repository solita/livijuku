(ns juku.service.ely-hakemus
  (:require [juku.db.yesql-patch :as sql]
            [juku.db.database :refer [db with-transaction]]
            [juku.db.coerce :as coerce]
            [juku.db.sql :as dml]
            [juku.service.hakemus-core :as hc]
            [juku.service.paatos :as p]
            [juku.schema.ely-hakemus :as s]
            [slingshot.slingshot :as ss]
            [ring.util.http-response :as r]
            [clojure.java.io :as io]
            [common.collection :as coll]
            [common.string :as xstr]
            [clojure.string :as str]
            [common.core :as c]
            [juku.service.pdf :as pdf]
            [common.map :as m]))

; *** ELY-hakemuksen tietoihin liittyvät kyselyt ***
(sql/defqueries "ely.sql")

; *** ELY-hakemus-skeemaan liittyvät konversiot tietokannan tietotyypeistä ***
(def coerce-maararahatarve (coerce/coercer s/Maararahatarve))
(def coerce-kehityshanke (coerce/coercer s/Kehityshanke))

; *** Virheviestit tietokannan rajoitteista ***
(def maararahatarve-constraint-errors
  {:maararahatarve_pk {:http-response r/bad-request
                        :message "Määrärahatarve {maararahatarvetyyppitunnus} on jo olemassa hakemuksella (id = {hakemusid})."}
   :maararahatarve_hakemus_fk {:http-response r/not-found
                                :message "Määrärahatarpeen {maararahatarvetyyppitunnus} hakemusta (id = {hakemusid}) ei ole olemassa."}
   :mrtarve_mrtarvetyyppi_fk {:http-response r/not-found
                              :message "Määrärahatarvetyyppiä {maararahatarvetyyppitunnus} ei ole olemassa."}})

(def kehityshanke-constraint-errors
  {:kehityshanke_pk {:http-response r/bad-request
                     :message "Kehityshanke {numero} on jo olemassa hakemuksella (id = {hakemusid})."}
   :kehityshanke_hakemus_fk {:http-response r/not-found
                             :message "Kehityshankkeen {numero} hakemusta (id = {hakemusid}) ei ole olemassa."}})

; *** Määrärahatarpeiden toiminnot ***
(defn update-maararahatarve [hakemusid maararahatarve]
  (dml/update-where! db "maararahatarve"
                     (dissoc maararahatarve :maararahatarvetyyppitunnus)
                     (assoc (select-keys maararahatarve [:maararahatarvetyyppitunnus]) :hakemusid hakemusid)))

(defn save-maararahatarpeet! [hakemusid maararahatarpeet]
  (hc/assert-edit-hakemus-content-allowed*! (hc/get-hakemus hakemusid))

  (with-transaction
    (doseq [maararahatarve maararahatarpeet]
      (dml/assert-update
        (update-maararahatarve hakemusid maararahatarve)
        (ss/throw+ {:http-response r/not-found
                    :message (str "Määrärahatarvetyyppiä " (:maararahatarvetyyppi maararahatarve)
                                  " ei ole olemassa hakemukselle: " hakemusid)}))))
  nil)

(defn find-hakemus-maararahatarpeet [hakemusid]
  (hc/assert-view-hakemus-content-allowed*! (hc/get-hakemus hakemusid))

  (map (comp coerce-maararahatarve (c/partial-first-arg m/dissoc-if-nil :tulot))
       (select-hakemus-maararahatarve {:hakemusid hakemusid})))

(defn find-maararahatarvetyypit [] (select-maararahatarvetyypit))

; *** Kehityshankkeiden toiminnot ***
(defn- insert-kehityshanke [kehityshanke]
  (:id (dml/insert db "kehityshanke" kehityshanke kehityshanke-constraint-errors kehityshanke)))

(defn save-kehityshankkeet! [hakemusid kehityshankkeet]
  (hc/assert-edit-hakemus-content-allowed*! (hc/get-hakemus hakemusid))

  (with-transaction
    (delete-hakemus-kehityshanke! {:hakemusid hakemusid})
    (doseq [kehityshanke kehityshankkeet]
      (insert-kehityshanke (assoc kehityshanke :hakemusid hakemusid))))
  nil)

(defn find-hakemus-kehityshankkeet [hakemusid]
  (hc/assert-view-hakemus-content-allowed*! (hc/get-hakemus hakemusid))

  (map coerce-kehityshanke (select-hakemus-kehityshanke {:hakemusid hakemusid})))

; *** ELY hakemus pdf dokumentin tiedot ***

(defn maarahatarpeet-section [maararahatarpeet]
  (let [maararahatarvetyypit (find-maararahatarvetyypit)
        maararahatarpeet+nimi (coll/assoc-join maararahatarpeet :nimi
                                               maararahatarvetyypit
                                               {:maararahatarvetyyppitunnus :tunnus}
                                               (comp :nimi first coll/children))
        template-values (map (comp
                               (c/partial-first-arg update :sidotut pdf/format-number)
                               (c/partial-first-arg update :uudet pdf/format-number)
                               (c/partial-first-arg update :tulot (c/nil-safe pdf/format-number)))
                             maararahatarpeet+nimi)

        maarahatarve-template (slurp (io/reader (io/resource (str "pdf-sisalto/templates/maararahatarve.txt"))))
        tulot-template "\t - Kauden tulot\t\t\t\t{tulot} e"
        template (fn [maararahatarve] (if (:tulot maararahatarve)
                                        (str maarahatarve-template "\n" tulot-template)
                                        maarahatarve-template))]
    (str/join "\n" (map (fn [maararahatarve] (xstr/interpolate (template maararahatarve) maararahatarve))
                        template-values))))

(defn kehityshankkeet-section [kehityshankkeet]
  (if (empty? kehityshankkeet)
    "\tEi kehityshankkeita"
    (let [kehityshanke-template "\t{nimi}\t\t\t\t{arvo} e"]
      (str/join "\n" (map (partial xstr/interpolate kehityshanke-template)
                          (map (c/partial-first-arg update :arvo pdf/format-number) kehityshankkeet))))))

(defn ely-template-values [hakemus]
  (let [hakemusid (:id hakemus)
        maararahatarpeet (find-hakemus-maararahatarpeet hakemusid)
        kehityshankkeet (find-hakemus-kehityshankkeet hakemusid)
        ely-hakemus (:ely (coerce/row->object (first (select-ely-hakemus {:hakemusid hakemusid}))))
        haettuavustus
          (+ (reduce (fn [acc x] (+ acc (- (+ (:sidotut x) (:uudet x)) (or (:tulot x) 0)))) 0 maararahatarpeet)
             (reduce (fn [acc x] (+ acc (:arvo x) 0)) 0 kehityshankkeet)
             (or (:siirtymaaikasopimukset ely-hakemus) 0)
             (or (:joukkoliikennetukikunnat ely-hakemus) 0))]
    (merge ely-hakemus
      {:maararahatarpeet (maarahatarpeet-section maararahatarpeet)
       :kehityshankkeet (kehityshankkeet-section kehityshankkeet)
       :haettuavustus haettuavustus})))

; *** Perustiedot ***
(defn save-elyhakemus [hakemusid elyhakemus]
  (hc/assert-edit-hakemus-content-allowed*! (hc/get-hakemus hakemusid))

  (dml/assert-update
    (dml/update-where! db "hakemus" (coerce/object->row {:ely elyhakemus}) {:id hakemusid})
    (ss/throw+ {:http-response r/not-found
                :message (str "Hakemusta " hakemusid " ei ole olemassa.")})))

(defn find-any-ely-paatos [vuosi]
  (let [hakemusid (-> {:vuosi vuosi :hakemustyyppitunnus "ELY"}
                      p/select-hakemukset-from-kausi first :id)]
    (p/find-current-paatos hakemusid)))