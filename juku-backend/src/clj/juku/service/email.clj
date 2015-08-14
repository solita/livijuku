(ns juku.service.email
  (:require [juku.db.yesql-patch :as sql]
            [postal.core :as mail]
            [common.string :as strx]
            [common.core :as c]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [juku.settings :refer [settings]]
            [common.string :as strx]
            [clojure.tools.logging :as log])
  (:import (javax.activation DataHandler)
           (javax.mail.util ByteArrayDataSource)))

(sql/defqueries "email.sql")

(def default-request
  {:from (get-in settings [:email :from])})

(def server
  {:host (str/trim (get-in settings [:email :server]))
   :port (get-in settings [:email :port])})

#_(def server
  {:host "solita-service-1.solita.fi"
   :port 25})

(defn send-multipart [to subject & parts]
  (if (and (not= (:email settings) "off")
           (not-empty to))
    (let [message (assoc default-request
                    :to to
                    :subject subject
                    :body parts)]

      (try
        (let [status (mail/send-message server message)]
          (if (not= (:code status) 0)
            (log/error (str "Sähköpostin lähettäminen epäonnistui - virhe: " status))
            (log/info (str "Sähköpostin lähettäminen onnistui - vastaanottajat: " (str/join ", " to) ", subject: " subject))))
        (catch Exception e (log/error e "Sähköpostin lähettäminen epäonnistui"))))))

(defn txt-part [message] {:type "text/plain; charset=utf-8"
                          :content message})

(defn post
  ([to subject message] (send-multipart to subject (txt-part message)))
  ([to subject message asiakirja-nimi asiakirja]
    (send-multipart to subject
                    (txt-part message)
                    {:type :attachment
                     :content asiakirja
                     :file-name asiakirja-nimi
                     :content-type "application/pdf"})))
                     ;:content-transfer-encoding "base64" java mail api päättelee tämän itse


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

(def asiakirja-nimi
  {:ah0 {:p  "Avustuspaatos-{vuosi}.pdf"}
   :mh1 {:p  "Maksatuspaatos-{vuosi}-1.pdf"}
   :mh2 {:p  "Maksatuspaatos-{vuosi}-2.pdf"}})

(defn- to-keyword [txt]
  (-> txt str/lower-case keyword))

(defn send-hakemustapahtuma-message [hakemus hakemustilatunnus asiakirja]
  (c/if-let*
    [to (set (filter (comp not str/blank?) (map :sahkoposti (select-organisaatio-emails (select-keys hakemus [:organisaatioid])))))
     hakemustyyppi (to-keyword (:hakemustyyppitunnus hakemus))
     hakemustila (to-keyword hakemustilatunnus)
     template-key [hakemustyyppi hakemustila]
     subject-template (get-in subjects template-key)
     body-template (get-in messages template-key)
     subject (strx/interpolate subject-template hakemus)
     message (str (strx/interpolate body-template hakemus) "\n\n"
                  linkki "\n\n"
                  footer)]

    (c/if-let* [asiakirja-nimi-template (get-in asiakirja-nimi template-key)
                asiakirja asiakirja]
      (post to subject message (strx/interpolate asiakirja-nimi-template hakemus) asiakirja)
      (post to subject message))
    nil))

(def byte-array-class (Class/forName "[B"))

(defn ^javax.mail.internet.MimeBodyPart create-mimebodypart [{:keys [content type content-type]}]
  (let [type (or content-type type)]
    (condp instance? content
           java.io.InputStream (doto (javax.mail.internet.MimeBodyPart.)
                                 (.setDataHandler (DataHandler. (ByteArrayDataSource. content type))))

           java.io.File (doto (javax.mail.internet.MimeBodyPart.)
                          (.attachFile (io/file content)))

           byte-array-class (doto (javax.mail.internet.MimeBodyPart.)
                              (.setDataHandler (DataHandler. (ByteArrayDataSource. content type))))

           (doto (javax.mail.internet.MimeBodyPart.)
             (.setContent content type)))))

(defn eval-bodypart [part]
  (let [mimebodypart (create-mimebodypart part)]
    (when (#{:inline :attachment} (:type part))
      (.setDisposition mimebodypart (name (:type part))))
    (when (:file-name part)
      (.setFileName mimebodypart (:file-name part)))
    (when (:content-type part)
      (.setHeader mimebodypart "Content-Type" (:content-type part)))
    (when (:content-transfer-encoding part)
      (.setHeader mimebodypart "Content-Transfer-Encoding" (:content-transfer-encoding part)))
    (when (:content-id part)
      (.setContentID mimebodypart (str "<" (:content-id part) ">")))
    (when (:description part)
      (.setDescription mimebodypart (:description part)))
    mimebodypart))

(extend-protocol postal.message/PartEval
  clojure.lang.IPersistentMap
  (eval-part [part] (eval-bodypart part)))