(ns juku.service.asiahallinta
  (:require [clj-http.client :as client]
            [juku.user :as current-user]
            [juku.headers :as h]
            [clj-time.core :as time]
            [schema.core :as s]
            [cheshire.core :as json]
            [ring.swagger.core]
            [common.string :as str]
            [common.string :as strx]
            [slingshot.slingshot :as ss]
            [clojure.set :as set]
            [ring.util.codec :as codec]
            [clojure.tools.logging :as log]
            [juku.settings :refer [settings]]
            [juku.settings :refer [asiahallinta-on?]])
  (:import (java.util UUID)
           (org.joda.time DateTime)))

(defn default-request [operation]
   {:basic-auth [(get-in settings [:asiahallinta :user])
                 (get-in settings [:asiahallinta :password])]

    :headers {"SOA-Kutsuja" "JUKU"
              "SOA-Kohde" "ASHA"
              "SOA-ViestiID" (str (UUID/randomUUID))
              "SOA-KayttajanID" current-user/*current-user-id*
              "SOA-Aikaleima" (str (time/now))
              "SOA-Toiminto" operation}

    :debug false
    :throw-entire-message true
    :socket-timeout 130000    ;; in milliseconds
    :conn-timeout 130000})    ;; in milliseconds

(s/defschema Hakemuskausi {:asianNimi             s/Str
                           :omistavaOrganisaatio  s/Str
                           :omistavaHenkilo       s/Str})

(s/defschema Hakemus {:kausi                 s/Str
                      :tyyppi                s/Str
                      :hakija                s/Str
                      :omistavaOrganisaatio  s/Str
                      :omistavaHenkilo       s/Str})

(s/defschema Taydennyspyynto {:maaraaika   DateTime
                              :kasittelija s/Str
                              :hakija      s/Str})

(s/defschema Taydennys {:kasittelija s/Str
                        :lahettaja      s/Str})

(s/defschema Hakuohje {:kasittelija s/Str})

(s/defschema Paatos {:paattaja s/Str})

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

(defn wrap-exception* [f]
  (ss/try+
    (f)
    (catch map? e (ss/throw+ {:type :arkistointi
                              :message (str "Asiahallintajärjestelmän tuottama virheviesti: ("
                                            (:status e) ") "
                                            (:body e))}))

    (catch Throwable t (ss/throw+ {:type :arkistointi
                                   :message (str "Asiahallintajärjestelmästä ei saatu vastausta. Yhteysvirhe: "
                                            (type t) " - " (.getMessage t))}))))

(defmacro wrap-exception [& body] `(wrap-exception* (fn [] ~@body)))

(defn- post-with-liitteet [path operation json-part-name json-schema json-object liitteet]

  (if (asiahallinta-on?)
    (let [json-part {:name json-part-name
                     :content (to-json json-schema json-object)
                     :mime-type "application/json"
                     :encoding "utf-8"}

        parts (cons json-part (map #(update-in % [:name] h/encode-value) liitteet))
        request (assoc (default-request operation) :multipart parts)
        url (str (get-in settings [:asiahallinta :url]) "/" path)]

      (log/info "post multipart" url request)
      (log-time (str "post " path) (wrap-exception (client/post url request))))

    (log/info "Asiahallinta ei ole päällä - toimenpide: " operation " viesti (" json-part-name "):" json-object)))

(defn- post [path operation json-schema json-object]

  (if (asiahallinta-on?)
    (let [request (assoc (default-request operation)
                    :content-type :json
                    :body (to-json json-schema json-object))
          url (str (get-in settings [:asiahallinta :url]) "/" path)]

      (log/info "post" url request)
      (log-time (str "post " path) (wrap-exception (client/post url request))))

    (log/info "Asiahallinta ei ole päällä - toimenpide: " operation " viesti:" json-object)))

(defn- put [path operation]

  (if (asiahallinta-on?)
    (let [request (default-request operation)
          url (str (get-in settings [:asiahallinta :url]) "/" path)]

      (log/info "put" url request)
      (log-time (str "put " path) (wrap-exception (client/put url request))))

    (log/info "Asiahallinta ei ole päällä - toimenpide: " operation )))

(defn rename-content-keys [content] (set/rename-keys content {:sisalto :content :contenttype :mime-type :nimi :name}))

(defn hakuohje-asiakirja-multipart [hakuohje]
  (assoc (rename-content-keys hakuohje) :part-name "hakuohje-asiakirja"
                                        :name "hakuohje.pdf"))

(defn avaa-hakemuskausi [hakemuskausi hakuohje]
  (strx/trim (:body (post-with-liitteet
                     "hakemuskausi" "AvaaKausi" "hakemuskausi"
                     Hakemuskausi (merge hakemuskausi omistaja)
                     [(hakuohje-asiakirja-multipart hakuohje)]))))

(defn hakemus-asiakirja-multipart [asiakirja]
  {:part-name "hakemus-asiakirja"
   :name "hakemus.pdf"
   :content asiakirja
   :mime-type "application/pdf"})

(defn hakemus-vireille [hakemus hakemusasiakirja liitteet]
  (str/trim (:body (post-with-liitteet
                     "hakemus" "Vireilla" "hakemus" Hakemus (merge hakemus omistaja)
                     (cons (hakemus-asiakirja-multipart hakemusasiakirja)
                           (map rename-content-keys liitteet))))))

(defn taydennyspyynto [diaarinumero taydennyspyynto]
  (post (str "hakemus/" (codec/url-encode diaarinumero) "/taydennyspyynto")
         "Taydennyspyynto"
         Taydennyspyynto taydennyspyynto))

(defn taydennys [diaarinumero taydennys hakemusasiakirja liitteet]
  (post-with-liitteet (str "hakemus/" (codec/url-encode diaarinumero) "/taydennys")
       "Taydennys" "taydennys" Taydennys taydennys
       (cons (hakemus-asiakirja-multipart hakemusasiakirja)
             (map rename-content-keys liitteet))))

(defn maksatushakemus [diaarinumero maksatushakemus hakemusasiakirja liitteet]
  (post-with-liitteet (str "hakemus/" (codec/url-encode diaarinumero) "/maksatushakemus")
        "Vireille" "maksatushakemus" Taydennys maksatushakemus
        (cons (hakemus-asiakirja-multipart hakemusasiakirja)
              (map rename-content-keys liitteet))))

(defn tarkastettu [diaarinumero]
  (put (str "hakemus/" (codec/url-encode diaarinumero) "/tarkastettu") "Tarkastettu"))

(defn kasittelyssa [diaarinumero]
  (put (str "hakemus/" (codec/url-encode diaarinumero) "/kasittely") "Kasittelyssa"))

(defn paatos [diaarinumero paatos paatosasiakirja]
  (post-with-liitteet (str "hakemus/" (codec/url-encode diaarinumero) "/paatos")
      "Paatos" "paatos" Paatos paatos
      [{:part-name "paatos-asiakirja"
        :name "paatos.pdf"
        :content paatosasiakirja
        :mime-type "application/pdf"}]))

(defn sulje-hakemuskausi [diaarinumero]
  (put (str "hakemuskausi/" (codec/url-encode diaarinumero) "/sulje") "SuljeKausi"))

(defn update-hakuohje [diaarinumero hakuohje hakuohje-asiakirja]
  (post-with-liitteet (str "hakemuskausi/" (codec/url-encode diaarinumero) "/hakuohje")
                      "HakuohjeTaydennys" "hakuohje" Hakuohje hakuohje
                      [(hakuohje-asiakirja-multipart hakuohje-asiakirja)]))


