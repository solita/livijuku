(ns juku.service.email
  (:require [juku.db.yesql-patch :as sql]
            [postal.core :as mail]
            [common.string :as strx]
            [common.collection :as coll]
            [common.core :as c]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [juku.settings :refer [settings]]
            [common.string :as strx]
            [clojure.tools.logging :as log]
            [juku.service.user :as user]
            [juku.service.organisaatio :as o])
  (:import (javax.activation DataHandler)
           (javax.mail.util ByteArrayDataSource)))

(sql/defqueries "email.sql")

(def default-request
  {:from (get-in settings [:email :from])})

(def server
  {:host (strx/trim (get-in settings [:email :server]))
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
  "Sähköpostin lähetyspalvelu, joka tukee kahta eri viestimuotoa:
  a) pelkkä tekstisisältö
  b) tekstisisältö + yksi liitedokumentti

  - Liitteet -

  Liitteiden lähetyksessä käytettävä content-transfer-encoding kannattaa jättää
   java mail api:n päätettäväksi esim. puhdas binäärimuoto näyttäisi toimivan huonosti ks.
   - http://www.w3.org/Protocols/rfc1341/5_Content-Transfer-Encoding.html
   - http://superuser.com/questions/402193/why-is-base64-needed-aka-why-cant-i-just-email-a-binary-file
   - https://en.wikipedia.org/wiki/Extended_SMTP
   - https://en.wikipedia.org/wiki/8-bit_clean
   - http://tools.ietf.org/html/rfc821
   - https://en.wikipedia.org/wiki/MIME
   - https://tools.ietf.org/html/rfc2231

   Liitteen tiedostonimessä ei toimi ääkköset."
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

(defn template-key [hakemus hakemustilatunnus]
  (c/if-let* [hakemustyyppi (to-keyword (:hakemustyyppitunnus hakemus))
              hakemustila (to-keyword hakemustilatunnus)]
             [hakemustyyppi hakemustila]
             nil))

(defn send-hakija-message [hakemus hakemustilatunnus asiakirja]
  (c/if-let*
    [to (set (filter (comp not str/blank?) (map :sahkoposti (select-organisaatio-emails (select-keys hakemus [:organisaatioid])))))
     template-key (template-key hakemus hakemustilatunnus)
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

(def kasittelija-messages
  {:ah0 {:v "Joukkoliikenteen valtionavustushakemus vuodelle {vuosi} on saapunut JUKU-järjestelmään."
         :tv "Joukkoliikenteen valtionavustushakemuksen täydennys on saapunut JUKU-järjestelmään."}
   :mh1 {:v "Joukkoliikenteen 1. maksatushakemus vuodelle {vuosi} on saapunut JUKU-järjestelmään."
         :tv "Joukkoliikenteen 1. maksatushakemuksen täydennys on saapunut JUKU-järjestelmään."}
   :mh2 {:v "Joukkoliikenteen 2. maksatushakemus vuodelle {vuosi} on saapunut JUKU-järjestelmään."
         :tv "Joukkoliikenteen 2. maksatushakemuksen täydennys on saapunut JUKU-järjestelmään."}})

(def kasittelija-subjects
  {:ah0 {:v  "{organisaatio} - avustushakemus {vuosi} on saapunut"
         :tv "{organisaatio} - avustushakemus {vuosi} täydennys"}
   :mh1 {:v  "{organisaatio} - maksatushakemus {vuosi}/1 on saapunut"
         :tv "{organisaatio} - maksatushakemus {vuosi}/1 täydennys"}
   :mh2 {:v  "{organisaatio} - maksatushakemus {vuosi}/2 on saapunut"
         :tv "{organisaatio} - maksatushakemus {vuosi}/2 täydennys"}})

(defn send-kasittelija-message [hakemus hakemustilatunnus _]
  (c/if-let*
    [to (set (filter (comp not str/blank?) (map :sahkoposti (filter (coll/eq :sahkopostiviestit true) (user/find-all-livi-users)))))
     template-key (template-key hakemus hakemustilatunnus)
     subject-template (get-in kasittelija-subjects template-key)
     body-template (get-in kasittelija-messages template-key)
     organisaatio (o/find-organisaatio (:organisaatioid hakemus))
     hakemus+org (assoc hakemus :organisaatio (:nimi organisaatio))
     subject (strx/interpolate subject-template hakemus+org)
     message (str "Hakija: " (:nimi organisaatio) "\n\n"
                  (strx/interpolate body-template hakemus+org))]
    (post to subject message)
    nil))

(defn send-hakemustapahtuma-message [hakemus hakemustilatunnus asiakirja]
  (send-hakija-message hakemus hakemustilatunnus asiakirja)
  (send-kasittelija-message hakemus hakemustilatunnus asiakirja))


(def byte-array-class (Class/forName "[B"))

(defn ^javax.mail.internet.MimeBodyPart create-mimebodypart

  "Binary content mimebodypart example see:
   https://vangjee.wordpress.com/2010/11/02/how-to-create-an-in-memory-pdf-report-and-send-as-an-email-attachment-using-itext-and-java/"

  [{:keys [content type content-type]}]
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