(ns juku.service.email
  (:require [juku.db.yesql-patch :as sql]
            [postal.core :as mail]
            [common.string :as strx]
            [common.core :as c]
            [clojure.string :as str]
            [juku.settings :refer [settings]]
            [common.string :as strx]
            [clojure.tools.logging :as log]))

(sql/defqueries "email.sql")

(def default-request
  {:from (get-in settings [:email :from])})

(def server
  {:host (get-in settings [:email :server])
   :port (get-in settings [:email :port])})

(defn post [to subject body]
  (if (and (not= (:email settings) "off")
           (not-empty to))
    (let [message (assoc default-request
                    :to to
                    :subject subject
                    :body [{:type "text/plain; charset=utf-8"
                           :content body}])]

      (try
        (let [status (mail/send-message server message)]
          (if (not= (:code status) 0)
            (log/error (str "Sähköpostin lähettäminen epäonnistui - virhe: " status))
            (log/info (str "Sähköpostin lähettäminen onnistui - vastaanottajat: " (str/join ", " to) ", subject: " subject))))
        (catch Exception e (log/error e "Sähköpostin lähettäminen epäonnistui"))))))

(def footer "Tämä on automaattinen viesti. Ole hyvä, älä vastaa tähän viestiin, sillä tähän osoitteeseen lähetettyjä viestejä ei käsitellä.")

(def linkki
  (str "Tarkemmat tiedot löydätte kirjautumalla seuraavasta linkistä järjestelmään:\n\n"
       "http:\\\\juku.liikennevirasto.fi"))

(def messages
  {:ah0 {:v "Joukkoliikenteen valtionavustushakemuksenne vuodelle {vuosi} on saapunut JUKU-järjestelmään."
         :t0 "Joukkoliikenteen valtionavustushakemuksenne vuodelle {vuosi} on palautettu täydennettäväksi."
         :tv "Joukkoliikenteen valtionavustushakemuksenne täydennys on saapunut JUKU-järjestelmään."
         :p "Joukkoliikenteen valtionavustushakemukseenne vuodelle {vuosi} on tehty päätös."}
   :mh1 {:v "Joukkoliikenteen 1. maksatushakemuksenne vuodelle {vuosi} on saapunut JUKU-järjestelmään."
         :t0 "Joukkoliikenteen 1. maksatushakemuksenne vuodelle {vuosi} on palautettu täydennettäväksi."
         :tv "Joukkoliikenteen 1. maksatushakemuksenne täydennys on saapunut JUKU-järjestelmään."
         :p "Joukkoliikenteen 1. maksatushakemukseenne vuodelle {vuosi} on tehty päätös."}
   :mh2 {:v "Joukkoliikenteen 2. maksatushakemuksenne vuodelle {vuosi} on saapunut JUKU-järjestelmään."
         :t0 "Joukkoliikenteen 2. maksatushakemuksenne vuodelle {vuosi} on palautettu täydennettäväksi."
         :tv "Joukkoliikenteen 2. maksatushakemuksenne täydennys on saapunut JUKU-järjestelmään."
         :p "Joukkoliikenteen 2. maksatushakemukseenne vuodelle {vuosi} on tehty päätös."}})

(def subjects
  {:ah0 {:v  "Avustushakemus {vuosi} on saapunut"
         :t0 "Avustushakemus {vuosi} täydennyspyyntö"
         :tv "Avustushakemus {vuosi} täydennys"
         :p  "Avustuspäätös {vuosi}"}
   :mh1 {:v  "Maksatushakemus {vuosi}/1 on saapunut"
         :t0 "Maksatushakemus {vuosi}/1 täydennyspyyntö"
         :tv "Maksatushakemus {vuosi}/1 täydennys"
         :p  "Maksatuspäätös {vuosi}/1"}
   :mh2 {:v  "Maksatushakemus {vuosi}/2 on saapunut"
         :t0 "Maksatushakemus {vuosi}/2 täydennyspyyntö"
         :tv "Maksatushakemus {vuosi}/2 täydennys"
         :p  "Maksatuspäätös {vuosi}/2"}})

(defn- to-keyword [txt]
  (-> txt str/lower-case keyword))

(defn send-hakemustapahtuma-message [hakemus hakemustilatunnus]
  (c/if-let*
    [to (set (filter (comp not str/blank?) (map :sahkoposti (select-organisaatio-emails (select-keys hakemus [:organisaatioid])))))
     hakemustyyppi (to-keyword (:hakemustyyppitunnus hakemus))
     hakemustila (to-keyword hakemustilatunnus)
     template-key [hakemustyyppi hakemustila]
     subject-template (get-in subjects template-key)
     body-template (get-in messages template-key)]

    (post to
          (strx/interpolate subject-template hakemus)
          (str (strx/interpolate body-template hakemus) "\n\n"
               linkki "\n\n"
               footer))
    nil))
