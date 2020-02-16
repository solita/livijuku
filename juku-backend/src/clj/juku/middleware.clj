(ns juku.middleware
  (:require [juku.user :as current-user]
            [juku.service.user :as user]
            [juku.service.organisaatio :as org]
            [common.map :as m]
            [buddy.sign.jwt :as jwt]
            [common.core :as c]
            [compojure.api.meta :as meta]
            [common.collection :as coll]
            [clojure.string :as str]
            [clojure-csv.core :as csv]
            [common.string :as strx]
            [juku.service.common :as sc]
            [slingshot.slingshot :as ss]
            [clojure.walk :as w]
            [ring.util.http-response :as r]
            [compojure.api.middleware :as cm]
            [ring.middleware.format-params :as rmf]
            [ring.middleware.format.impl :as rmf-impl]
            [ring.middleware.anti-forgery :as af]
            [ring.swagger.middleware :as rm]
            [juku.headers :as h]
            [clojure.tools.logging :as log]
            [crypto.random :as random]
            [compojure.api.exception :as ex])
  (:import (java.io Serializable)))

(defn normalize-name [name]
  (-> name str/trim str/lower-case (str/replace #"(\s)+" "-")))

(defn find-matching-organisaatio [organisaatio-name department-name]
  (c/if-let* [normalized-org-name (normalize-name organisaatio-name)
              normalized-dep-name (c/maybe-nil normalize-name "-" department-name)

              organisaatio (org/find-unique-organisaatio-ext-tunnus-like (str normalized-org-name "/" normalized-dep-name))]
           (:id organisaatio) nil))

(defn- headers->user-data [orgnisaatio-id headers]
  (assoc (m/dissoc-if {:etunimi (strx/trim (h/parse-header headers :givenname))
                       :sukunimi (strx/trim (h/parse-header headers :sn))
                       :sahkoposti (strx/trim (h/parse-header headers :mail))} str/blank?)
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
  (log/error (str/join msg))
  (h/content-type-text-plain (type (str/join msg))))

(defn with-user* [user fn]
  (current-user/with-user-id (:tunnus user) (user/with-user* user fn)))

(defmacro with-user [user & body] `(with-user* ~user (fn [] ~@body)))

(defn remove-quotes [txt]
  (str/replace txt "\"" ""))

(defn wrap-user [handler]
  (fn [request]
    (c/if-let3
      [original-headers (c/nil-if empty? (:headers request))
        (error r/bad-request "Pyynnössä ei ole otsikkotietoa.")
       headers (c/nil-if (coll/predicate not= count (count original-headers)) (m/keys-to-keywords original-headers))
        (error r/bad-request "Pyynnön otsikkotiedossa on päällekkäisiä otsikoita.")
       uid (:iv-user headers)
        (error r/bad-request "Käyttäjätunnusta ei löydy pyynnön otsikkotiedosta: iv-user.")
       group-txt (:iv-groups headers)
        (error r/bad-request "Käyttäjän " uid " käyttäjäryhmiä ei ole pyynnön otsikkotiedossa: iv-groups.")
       roles (c/nil-if empty? (user/find-roleids (map (comp remove-quotes str/trim) (str/split group-txt #","))))
        (error r/forbidden "Käyttäjällä " uid " ei ole yhtään juku-järjestelmän käyttäjäroolia - iv-groups: " group-txt)
       organisaatio-name (h/parse-header headers :o nil)
        (error r/bad-request "Käyttäjän " uid " organisaation nimeä ei löydy pyynnön otsikkotiedosta: o.")
       orgnisaatio-id (find-matching-organisaatio organisaatio-name (h/parse-header headers :ou))
        (error r/forbidden "Käyttäjän " uid " organisaatiota: " organisaatio-name
              " (osasto: " (h/parse-header headers :ou) ") ei tunnisteta.")
       privileges (c/nil-if empty? (user/find-privileges roles orgnisaatio-id))
        (error r/forbidden "Käyttäjällä " uid " ei ole voimassaolevaa käyttöoikeutta järjestelmään.")]

      (with-user (assoc (sc/retry 2 save-user uid orgnisaatio-id roles headers)
                        :privileges privileges) (handler request)))))

(def guest-user {:tunnus "guest"
                 :privileges [:view-tunnusluvut :view-kilpailutus]})

(defn wrap-public-user [handler]
  (fn [request] (with-user guest-user (handler request))))

(defn ^String message [^Throwable t] (.getMessage t))

(defn ^Throwable cause [^Throwable t] (.getCause t))

(defn service-name [request]
  (str ((c/nil-safe str/upper-case) (name (:request-method request))) " " (:uri request)))

(defn remove-non-serializable-values [object]
  (w/prewalk
    #(if (or (and (map-entry? %) (not (instance? Serializable (second %))))
             (not (instance? Serializable %)))
       nil %)
    object))

(defn classname [object]
  (.getName (class object)))

(defn throwable->http-error [^Throwable t]
  (if t
    (let [error (or (ss/get-thrown-object t) (ex-data t) {})]
      (merge-with #(or %1 %2)
        (select-keys error [:message :type :http-response])
        {:message (message t)
         :type (classname t)}))))

(defn exception-handler [exception _ request]
  (let [error (throwable->http-error exception)
        http-response (or (:http-response error)
                          (if (isa? (:type error) ::coll/not-found) r/not-found)
                          r/internal-server-error)
        response (http-response (dissoc error :http-response))]
    (log/error exception
               (service-name request) " -> "
               (:status response))
    response))

(defn logging-wrapper [error-msg f]
  (fn [exception data request]
    (let [response (f exception data request)
          body (:body response)]
      (log/error (service-name request) "->" error-msg (or (:errors body)
                                                           (str (:type body) " - "
                                                                (:message body) )))
      response)))

(defn response-validation-handler [exception data request]
  (assoc-in (ex/response-validation-handler exception data request) [:body :type] "response-validation-error"))

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

(defn- validate-token [header cookie secret]
  (cond
    (not= header cookie)
      (str "Keksin XSRF-TOKEN arvo: " cookie
           " ei vastaa x-xsrf-token-otsikkotiedossa olevaa arvoa: " header)
    (nil? header) "Otsikko x-xsrf-token puuttuu."
    :else (try (jwt/unsign header secret) nil
               (catch Throwable t (.getMessage t)))))

(defn request-token [secret _]
  (assoc-in (r/content-type (r/ok) "application/javascript")
            [:cookies "XSRF-TOKEN"]
            {:value (jwt/sign {:id (random/base64 16)} secret)
             :http-only false :path "/"}))

(defn wrap-csrf-prevention

  "CSRF-hyökkäyksen esto ja json-haavoittovuuden suoja perustuu double submit cookie -malliin
  ja allekirjoitettuun token arvoon.

  Tässä mallissa käyttöliitymäsovellus todistaa olevansa oikea pystymällä lukemaan XSRF-TOKEN keksin
  kirjoittamalla se otsikkotietoon. Token on allekirjoitettu ja keksi pyydetään erillisellä pyynnöllä
  ennen suojattujen palveluiden käyttämistä.

  Kaikki suojatut palvelupyynnöt edellyttävät että tietojen:
  - x-xsrf-token (http-otsikko)
  - XSRF-TOKEN (keksin)
  arvot ovat samat ja allekirjoituksen validointi onnistuu.

  Lähteet:
  - https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html
  - https://docs.angularjs.org/api/ng/service/$http#security-considerations
  - https://en.wikipedia.org/wiki/Cross-site_request_forgery
  - http://security.stackexchange.com/questions/61110/why-does-double-submit-cookies-require-a-separate-cookie
  - http://security.stackexchange.com/questions/59470/double-submit-cookies-vulnerabilities
  - http://stackoverflow.com/questions/4463422/csrf-can-i-use-a-cookie
  - https://code.google.com/p/browsersec/wiki/Part2#Same-origin_policy_for_cookies"

  [secret whitelist handler]
  (fn [request]
    (let [header-token (get-in request [:headers "x-xsrf-token"])
          cookie-token (get-in request [:cookies "XSRF-TOKEN" :value])
          error-msg (validate-token header-token cookie-token secret)]

      (if (or (nil? error-msg)
              (some #(re-matches % (str (str/upper-case (name (:request-method request))) " " (:uri request)))
                    whitelist))
        (handler request)
        (do
          (log/error (str "XSRF error: " error-msg))
          (h/content-type-text-plain
            (r/forbidden (str "Taustapalvelu tunnisti mahdollisesti väärennetyn pyynnön. "
                              "Kokeile toimintoa uudestaan ja jos tämä virhe toistuu "
                              "niin lataa koko sivu uudestaan (Esim. chrome paina F5)."))))))))

; *** csv support ***

(def csv-request? (rmf/make-type-request-pred #"^text/csv"))

(defn wrap-csv-params
  "Handles body params in CSV format. See [[rmf/wrap-format-params]] for details."
  [handler & args]
  (let [options (rmf-impl/extract-options args)]
    (rmf/wrap-format-params
      handler (assoc options
                :predicate csv-request?
                :decoder (fn [csv]
                           (apply csv/parse-csv csv
                                  (apply concat (-> options :options))))))))

; *** A simple api key middleware ***

(defn wrap-api-key [api-key handler]
  (fn [request]
    (let [header-api-key (get-in request [:headers "x-api-key"])]
      (if (= api-key header-api-key)
        (handler request)
        (error r/forbidden (str "API key in x-api-key header is not valid"))))))