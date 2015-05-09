(ns juku.service.asiahallinta
  (:require [clj-http.client :as client]
            [juku.user :as current-user]
            [juku.headers :as h]
            [clj-time.core :as time]
            [schema.core :as s]
            [cheshire.core :as json]
            [ring.swagger.core]
            [common.string :as str]
            [clojure.set :as set]
            [ring.util.codec :as codec]
            [clojure.tools.logging :as log]
            [juku.settings :refer [settings]])
  (:import (java.util UUID)
           (org.joda.time LocalDate)))

(defn default-request [operation]
   {:basic-auth [(get-in settings [:asiahallinta :user])
                 (get-in settings [:asiahallinta :password])]

    :headers {"SOA-Kutsuja" "JUKU"
              "SOA-Kohde" "ASHA"
              "SOA-ViestiID" (str (UUID/randomUUID))
              "SOA-KayttajanID" current-user/*current-user-id*
              "SOA-Aikaleima" (str (time/now))
              "SOA-Toiminto" operation}

    :debug true
    :socket-timeout 60000    ;; in milliseconds
    :conn-timeout 60000})    ;; in milliseconds

(s/defschema Hakemuskausi {:asianNimi             s/Str
                           :omistavaOrganisaatio  s/Str
                           :omistavaHenkilo       s/Str})

(s/defschema Hakemus {:kausi                 s/Str
                      :hakija                s/Str
                      :omistavaOrganisaatio  s/Str
                      :omistavaHenkilo       s/Str})

(s/defschema Taydennyspyynto {:maaraaika   LocalDate
                              :kasittelija s/Str
                              :hakija      s/Str})

(s/defschema Taydennys {:kasittelija s/Str
                        :lahettaja      s/Str})

(def omistaja {:omistavaOrganisaatio "Liikennevirasto"
               :omistavaHenkilo (get-in settings [:asiahallinta :omistavahenkilo])})

(defn- log-time* [title f]
  (let [start (System/nanoTime)
        result (f)]
    (log/info title "elapsed time:" (/ (- (System/nanoTime) start) 1000000.0) "msec")
    result))

(defmacro log-time [title & body] `(log-time* ~title (fn [] ~@body)))

(defn to-json [schema object]
  (do
    (s/validate schema object)
    (json/generate-string object)))

(defn- post-with-liitteet [path operation json-part-name json-schema json-object liitteet]

  (if (not= (:asiahallinta settings) "off")
    (let [json-part {:name json-part-name
                     :content (to-json json-schema json-object)
                     :mime-type "application/json"
                     :encoding "utf-8"}

        parts (cons json-part (map #(update-in % [:name] h/encode-value) liitteet))
        request (assoc (default-request operation) :multipart parts)
        url (str (get-in settings [:asiahallinta :url]) "/" path)]

      (log/info "post multipart" url request)
      (log-time (str "post " path) (client/post url request)))

    (log/info "Asiahallinta ei ole päällä - toimenpide: " operation " viesti (" json-part-name "):" json-object)))

(defn- post [path operation json-schema json-object]

  (if (not= (:asiahallinta settings) "off")
    (let [request (assoc (default-request operation)
                    :content-type :json
                    :body (to-json json-schema json-object))
          url (str (get-in settings [:asiahallinta :url]) "/" path)]

      (log/info "post" url request)
      (log-time (str "post " path) (client/post url request)))

    (log/info "Asiahallinta ei ole päällä - toimenpide: " operation " viesti:" json-object)))

(defn- put [path operation]

  (if (not= (:asiahallinta settings) "off")
    (let [request (default-request operation)
          url (str (get-in settings [:asiahallinta :url]) "/" path)]

      (log/info "put" url request)
      (log-time (str "put " path) (client/put url request)))

    (log/info "Asiahallinta ei ole päällä - toimenpide: " operation )))

(defn rename-content-keys [content] (set/rename-keys content {:sisalto :content :contenttype :mime-type :nimi :name}))

(defn avaa-hakemuskausi [hakemuskausi hakuohje]
  (str/trim (:body (post-with-liitteet
                     "hakemuskausi" "AvaaKausi" "hakemuskausi"
                     Hakemuskausi (merge hakemuskausi omistaja)
                     [(assoc (rename-content-keys hakuohje) :part-name "hakuohje-asiakirja"
                                                            :name "hakuohje.pdf")]))))

(defn hakemus-vireille [hakemus hakemusasiakirja liitteet]
  (str/trim (:body (post-with-liitteet
                     "hakemus" "Vireilla" "hakemus" Hakemus (merge hakemus omistaja)
                     (cons {:part-name "hakemus-asiakirja"
                            :name "hakemus.pdf"
                            :content hakemusasiakirja
                            :mime-type "application/pdf"}
                           (map rename-content-keys liitteet))))))

(defn taydennyspyynto [diaarinumero taydennyspyynto]
  (post (str "hakemus/" (codec/url-encode diaarinumero) "/taydennyspyynto")
         "Taydennyspyynto"
         Taydennyspyynto taydennyspyynto))

(defn taydennys [diaarinumero taydennys hakemusasiakirja liitteet]
  (post-with-liitteet (str "hakemus/" (codec/url-encode diaarinumero) "/taydennys")
       "Taydennys" "taydennys" Taydennys taydennys
       (cons {:name "hakemus-asiakirja" :content hakemusasiakirja :mime-type "application/pdf"}
             (map rename-content-keys liitteet))))

(defn tarkastettu [diaarinumero]
  (put (str "hakemus/" (codec/url-encode diaarinumero) "/tarkastettu") "Tarkastettu"))

(defn sulje-hakemuskausi [diaarinumero]
  (put (str "hakemuskausi/" (codec/url-encode diaarinumero) "/sulje") "SuljeKausi"))


