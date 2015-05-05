(ns juku.service.asiahallinta
  (:require [clj-http.client :as client]
            [juku.user :as current-user]
            [clj-time.core :as time]
            [ring.swagger.schema :as swagger]
            [schema.core :as s]
            [cheshire.core :as json]
            [common.string :as str]
            [clojure.set :as set]
            [clojure.tools.logging :as log]
            [juku.settings :refer [settings]])
  (:import (java.util UUID)))

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

(s/defschema Hakemus {:kausi                 s/Int
                      :hakija                s/Str
                      :omistavaOrganisaatio  s/Str
                      :omistavaHenkilo       s/Str})

(def omistaja {:omistavaOrganisaatio "Liikennevirasto"
               :omistavaHenkilo (get-in settings [:asiahallinta :omistavahenkilo])})

(defn- post-with-liitteet [path operation json-part-name json-schema json-object liitteet]

  (if (not= (:asiahallinta settings) "off")
    (let [json-part {:name json-part-name
                   :content (json/generate-string (swagger/coerce! json-schema json-object))
                   :mime-type "application/json"
                   :encoding "utf-8"}

        parts (cons json-part liitteet)
        request (assoc (default-request operation) :multipart parts)
        url (str (get-in settings [:asiahallinta :url]) "/" path)]

    (log/info "post" url request)
    (client/post url request))

    (do
      (log/info "Asiahallinta ei ole päällä - toimenpide: " operation " viesti (" json-part-name "):" json-object))))

(defn- put [path operation]

  (if (not= (:asiahallinta settings) "off")
    (let [request (default-request operation)
          url (str (get-in settings [:asiahallinta :url]) "/" path)]

      (log/info "post" url request)
      (client/put url request))

    (do
      (log/info "Asiahallinta ei ole päällä - toimenpide: " operation ))))

(defn rename-content-keys [content] (set/rename-keys content {:sisalto :content :contenttype :mime-type :nimi :name}))

(defn avaa-hakemuskausi [hakemuskausi hakuohje]
  (str/trim (:body (post-with-liitteet
                     "hakemuskausi" "AvaaKausi" "hakemuskausi"
                     Hakemuskausi (merge hakemuskausi omistaja) [(assoc (rename-content-keys hakuohje) :name "hakuohje-asiakirja")]))))

(defn hakemus-vireille [hakemus hakemusasiakirja liitteet]
  (str/trim (:body (post-with-liitteet
                     "hakemus" "Vireilla"
                     "hakemus" Hakemus (merge hakemus omistaja)
                     (cons {:name "hakemus-asiakirja" :content hakemusasiakirja :mime-type "application/pdf"}
                           (map rename-content-keys liitteet))))))

(defn sulje-hakemuskausi [diaarinumero]
  (put (str "hakemuskausi/" diaarinumero "/sulje") "SuljeKausi"))


