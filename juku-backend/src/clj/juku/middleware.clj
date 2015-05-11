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
                       :sukunimi (strx/trim (h/parse-header headers :oam-user-last-name))} str/blank?)
                       :organisaatioid orgnisaatio-id))

(defn save-user [uid orgnisaatio-id headers]
  (let [user-data (headers->user-data orgnisaatio-id headers)]
    (if-let [user (user/find-user uid)]
      (let [updated-user (merge user user-data)]
        (if (not= user updated-user) (user/update-user! uid user-data))
        updated-user)
      (do
        (user/create-user! uid user-data)
        (assoc user-data :tunnus uid :jarjestelma false)))))

(defn- error [type & msg]
  (log/error msg)
  (r/content-type (type (apply str msg)) "text/plain; charset=UTF-8"))

(defn wrap-user [handler]
  (fn [request]
    (c/if-let3
      [headers (m/keys-to-keywords (:headers request))
        (error r/bad-request "Pyynnössä ei ole otsikkotietoa.")
       uid (:oam-remote-user headers)
        (error r/bad-request "Käyttäjätunnusta ei löydy pyynnön otsikkotiedosta: oam-remote-user.")
       group-txt (:oam-groups headers)
        (error r/bad-request "Käyttäjäryhmiä ei löydy pyynnön otsikkotiedosta: oam-groups.")
       organisaatio-name (h/parse-header headers :oam-user-organization nil)
        (error r/bad-request "Käyttäjän organisaation nimeä ei löydy pyynnön otsikkotiedosta: oam-user-organization.")
       privileges (coll/nil-if-empty (user/find-privileges (str/split group-txt #",")))
        (error r/forbidden "Käyttäjällä " uid " ei ole voimassaolevaa käyttöoikeutta järjestelmään.")
       orgnisaatio-id (find-matching-organisaatio organisaatio-name (h/parse-header headers :oam-user-department))
        (error r/forbidden "Käyttäjän " uid " organisaatiota: " organisaatio-name
              " (osasto: " (h/parse-header headers :oam-user-department) ") ei tunnisteta tai sitä ei löydetä yksikäsitteisesti.")]

      (current-user/with-user-id uid
        (user/with-user (assoc (sc/retry 1 save-user uid orgnisaatio-id headers) :privileges privileges) (handler request))))))

(defn- assoc-throw-context [map throw-context]
  (let [message (:message throw-context)
        cause (:cause throw-context)
        cause-message (c/maybe-nil #(.getMessage %) "" cause)]
    (assoc map :message message :cause cause-message)))


(defn- json-type-or-str [v] (if (or (keyword? v) (map? v) (vector? v) (number? v) (string? v) (instance? Boolean v)) v (str v)))

(defn- error->json [map] (w/postwalk json-type-or-str map))

;; more suitable exception info support
(defn ex-info-support
  [handler]
  (fn [request]
    (ss/try+
      (handler request)
      (catch :http-response {:keys [http-response] :as e}
        (let [error-body (error->json (dissoc e :http-response))]
          (log/info (:throwable &throw-context))
          (http-response (assoc-throw-context error-body &throw-context))))
      (catch map? e
        (let [error-body (error->json e)]
          (log/error (:throwable  &throw-context))
          (r/internal-server-error (assoc-throw-context error-body &throw-context))))
      (catch Throwable t
        (log/error t (.getMessage t))
        (throw t)))))

(alter-var-root #'cm/ex-info-support (constantly ex-info-support))


