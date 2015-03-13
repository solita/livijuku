(ns juku.middleware
  (:require [juku.user :as current-user]
            [juku.service.user :as user]
            [common.map :as m]
            [common.core :as c]
            [clojure.string :as str]
            [slingshot.slingshot :as ss]
            [clojure.walk :as w]
            [ring.util.http-response :as r]
            [compojure.api.middleware :as cm]
            [clojure.tools.logging :as log]))

(defn wrap-user [handler]
  (fn [request]
    (c/if-let* [headers (m/keys-to-keywords (:headers request))
                uid (:oam-remote-user headers)]
      (current-user/with-user-id uid
        (c/if-let* [group-txt (:oam-groups headers)
                    roles (str/split group-txt #",")
                    privileges (user/find-privileges roles)
                    user (user/find-user uid)]
               (user/with-user (assoc user :privileges privileges) (handler request))
               (r/forbidden (str "Käyttäjällä " uid " ei ole voimassaolevaa käyttöoikeutta järjestelmään."))))
      (r/forbidden "Käyttäjätunnusta ei löydy pyynnön otsikkotiedosta: oam-remote-user."))))

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


