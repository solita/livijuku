(ns juku.middleware
  (:require [juku.user :as current-user]
            [juku.service.user :as user]
            [juku.service.organisaatio :as org]
            [common.map :as m]
            [common.core :as c]
            [common.collection :as coll]
            [clojure.string :as str]
            [common.string :as strx]
            [juku.service.common :as sc]
            [slingshot.slingshot :as ss]
            [clojure.walk :as w]
            [ring.util.http-response :as r]
            [compojure.api.middleware :as cm]
            [juku.headers :as h]
            [clojure.tools.logging :as log]))

(defn find-matching-organisaatio [organisaatio-name department]
  (c/if-let* [normalized-org-name (-> organisaatio-name str/trim str/lower-case (str/replace #"(\s)+" "-"))

              organisaatio (org/find-unique-organisaatio-ext-tunnus-like (str normalized-org-name "/" (or department "-")))]
           (:id organisaatio) nil))

(defn- headers->user-data [orgnisaatio-id headers]
  (assoc (m/dissoc-if {:etunimi (strx/trim (h/parse-header headers :oam-user-first-name))
                       :sukunimi (strx/trim (h/parse-header headers :oam-user-last-name))
                       :sahkoposti (strx/trim (h/parse-header headers :oam-user-mail))} str/blank?)
                       :organisaatioid orgnisaatio-id))

(defn save-user [uid orgnisaatio-id roles headers]
  (let [user-data (headers->user-data orgnisaatio-id headers)]
    (if-let [user (user/find-user uid)]
      (let [updated-user (merge user user-data)]
        (if (not= user updated-user) (user/update-user! uid user-data))
        (user/update-roles! uid roles)
        updated-user)
      (do
        (user/create-user! uid user-data)
        (user/update-roles! uid roles)
        (user/find-user uid)))))

(defn- error [type & msg]
  (log/error msg)
  (h/content-type-text-plain (type (apply str msg))))

(defn wrap-user [handler]
  (fn [request]
    (c/if-let3
      [headers (m/keys-to-keywords (:headers request))
        (error r/bad-request "Pyynnössä ei ole otsikkotietoa.")
       uid (:oam-remote-user headers)
        (error r/bad-request "Käyttäjätunnusta ei löydy pyynnön otsikkotiedosta: oam-remote-user.")
       group-txt (:oam-groups headers)
        (error r/bad-request "Käyttäjäryhmiä ei löydy pyynnön otsikkotiedosta: oam-groups.")
       roles (c/nil-if empty? (user/find-roleids (str/split group-txt #",")))
        (error r/bad-request (str "Käyttäjäryhmillä ei löydy yhtään juku-järjestelmän käyttäjäroolia - oam-groups: " group-txt))
       organisaatio-name (h/parse-header headers :oam-user-organization nil)
        (error r/bad-request "Käyttäjän organisaation nimeä ei löydy pyynnön otsikkotiedosta: oam-user-organization.")
       privileges (c/nil-if empty? (user/find-privileges roles))
        (error r/forbidden "Käyttäjällä " uid " ei ole voimassaolevaa käyttöoikeutta järjestelmään.")
       orgnisaatio-id (find-matching-organisaatio organisaatio-name (h/parse-header headers :oam-user-department))
        (error r/forbidden "Käyttäjän " uid " organisaatiota: " organisaatio-name
              " (osasto: " (h/parse-header headers :oam-user-department) ") ei tunnisteta.")]

      (current-user/with-user-id uid
        (user/with-user (assoc (sc/retry 1 save-user uid orgnisaatio-id roles headers)
                          :privileges privileges) (handler request))))))

(defn ^String message [^Throwable t] (.getMessage t))

(defn ^Throwable cause [^Throwable t] (.getCause t))

(defn throwable->http-error [^Throwable t]
  (if t
    (let [error (or (ss/get-thrown-object t) (ex-data t))]
      (if (map? error)
        (assoc error
          :message (or (:message error) (message t))
          :cause (throwable->http-error (cause t)))
        {:message (message t)
         :cause (throwable->http-error (cause t))}))))

(defn exception-handler [exception]
  (let [error (throwable->http-error exception)
        http-response (or (:http-response error)
                          (if (isa? (:type error) ::coll/not-found) r/not-found)
                          r/internal-server-error)
        response (http-response (dissoc error :http-response))]
    (log/error exception (:status response))
    response))

(defn no-cache [response]
  (r/header response "Cache-Control" "no-store"))

(defn wrap-no-cache [handler]
  (fn [request]
    (let [response (handler request)]
      (if (or (r/get-header response "Cache-Control")
              (r/get-header response "Expires")
              (r/get-header response "Last-Modified")
              (r/get-header response "Etag"))
        response
        (no-cache response)))))



