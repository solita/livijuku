(ns juku.middleware
  (:require [juku.user :as current-user]
            [juku.service.user :as user]
            [juku.service.organisaatio :as org]
            [common.map :as m]
            [common.core :as c]
            [compojure.api.meta :as meta]
            [common.collection :as coll]
            [clojure.string :as str]
            [common.string :as strx]
            [juku.service.common :as sc]
            [slingshot.slingshot :as ss]
            [clojure.walk :as w]
            [ring.util.http-response :as r]
            [compojure.api.middleware :as cm]
            [ring.middleware.anti-forgery :as af]
            [ring.swagger.middleware :as rm]
            [juku.headers :as h]
            [clojure.tools.logging :as log]
            [crypto.random :as random]
            [compojure.api.exception :as ex]))

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
  (log/error (str/join msg))
  (h/content-type-text-plain (type (str/join msg))))

(defn wrap-user [handler]
  (fn [request]
    (c/if-let3
      [original-headers (c/nil-if empty? (:headers request))
        (error r/bad-request "Pyynnössä ei ole otsikkotietoa.")
       headers (c/nil-if (coll/predicate not= count (count original-headers)) (m/keys-to-keywords original-headers))
        (error r/bad-request "Pyynnön otsikkotiedossa on päällekkäisiä otsikoita.")
       uid (:oam-remote-user headers)
        (error r/bad-request "Käyttäjätunnusta ei löydy pyynnön otsikkotiedosta: oam-remote-user.")
       group-txt (:oam-groups headers)
        (error r/bad-request "Käyttäjän " uid " käyttäjäryhmiä ei ole pyynnön otsikkotiedossa: oam-groups.")
       roles (c/nil-if empty? (user/find-roleids (str/split group-txt #",")))
        (error r/forbidden "Käyttäjällä " uid " ei ole yhtään juku-järjestelmän käyttäjäroolia - oam-groups: " group-txt)
       organisaatio-name (h/parse-header headers :oam-user-organization nil)
        (error r/bad-request "Käyttäjän " uid " organisaation nimeä ei löydy pyynnön otsikkotiedosta: oam-user-organization.")
       privileges (c/nil-if empty? (user/find-privileges roles))
        (error r/forbidden "Käyttäjällä " uid " ei ole voimassaolevaa käyttöoikeutta järjestelmään.")
       orgnisaatio-id (find-matching-organisaatio organisaatio-name (h/parse-header headers :oam-user-department))
        (error r/forbidden "Käyttäjän " uid " organisaatiota: " organisaatio-name
              " (osasto: " (h/parse-header headers :oam-user-department) ") ei tunnisteta.")]

      (current-user/with-user-id uid
        (user/with-user (assoc (sc/retry 2 save-user uid orgnisaatio-id roles headers)
                          :privileges privileges) (handler request))))))

(defn ^String message [^Throwable t] (.getMessage t))

(defn ^Throwable cause [^Throwable t] (.getCause t))

(defn service-name [request]
  (str ((c/nil-safe str/upper-case) (name (:request-method request))) " " (:uri request)))

(defn throwable->http-error [^Throwable t]
  (if t
    (let [error (or (ss/get-thrown-object t) (ex-data t))]
      (if (map? error)
        (assoc error
          :message (or (:message error) (message t))
          :cause (throwable->http-error (cause t)))
        {:message (message t)
         :type (.getName ^Class (.getClass t))
         :cause (throwable->http-error (cause t))}))))

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

(defn wrap-double-submit-cookie

  "CSRF-hyökkäyksen esto ja json-haavoittovuuden suoja perustuu double submit cookie -malliin.
  Double submit cookie -mallissa sovellus todistaa olevansa oikea pystymällä joko kirjoittamaan tai lukemaan
  sovelluksen domainin sessiokeksejä. Kaikki pyynnöt edellyttävät että tietojen:
  - x-xsrf-token (http-otsikko)
  - XSRF-TOKEN (keksin)
  arvot ovat samat.

  Lähteet:
  - https://www.owasp.org/index.php/Cross-Site_Request_Forgery_(CSRF)_Prevention_Cheat_Sheet#Double_Submit_Cookies
  - https://en.wikipedia.org/wiki/Cross-site_request_forgery#Forging_login_requests
  - http://security.stackexchange.com/questions/61110/why-does-double-submit-cookies-require-a-separate-cookie
  - http://security.stackexchange.com/questions/59470/double-submit-cookies-vulnerabilities
  - http://stackoverflow.com/questions/4463422/csrf-can-i-use-a-cookie
  - https://code.google.com/p/browsersec/wiki/Part2#Same-origin_policy_for_cookies
  - https://docs.angularjs.org/api/ng/service/$http#security-considerations"

  [handler whitelist]
  (fn [request]
    (let [header-token (get-in request [:headers "x-xsrf-token"])
          cookie-token (get-in request [:cookies "XSRF-TOKEN" :value])]

      (if (or (= header-token cookie-token)
              (some #(re-matches % (str (str/upper-case (name (:request-method request))) " " (:uri request))) whitelist))
        (let [response (handler request)]
          (if ((c/nil-safe strx/substring?) "unsecure" cookie-token)
            (assoc-in response [:cookies "XSRF-TOKEN"] {:value (random/base64 60) :http-only false :path "/"})
            response))
        (error r/forbidden (str "Taustapalvelu tunnisti mahdollisesti väärennetyn pyynnön. Keksissä XSRF-TOKEN oleva arvo: "
                                cookie-token " ei vastaa x-xsrf-token-otsikkotiedossa olevaa arvoa: " header-token))))))


