(ns juku.middleware
  (:require [juku.user :as current-user]
            [juku.service.user :as user]
            [common.map :as m]
            [common.core :as c]
            [clojure.string :as str]
            [slingshot.slingshot :as ss]
            [ring.util.http-response :as r]
            [compojure.api.middleware :as cm]))

(defn wrap-user [handler]
  (fn [request]
    (c/if-let* [headers (m/keys-to-keywords (:headers request))
                uid (:oam-remote-user headers)]
      (current-user/with-user-id uid
        (c/if-let* [group-txt (:oam-groups headers)
                    roles (str/split group-txt #",")
                    user (user/find-user uid)]
               (user/with-user (assoc user :roles roles) (handler request))
               (r/forbidden (str "Käyttäjällä " uid " ei ole voimassaolevaa käyttöoikeutta järjestelmään."))))
      (r/forbidden "Käyttäjätunnusta ei löydy pyynnön otsikkotiedosta: oam-remote-user."))))

(defn- assoc-throw-context [map throw-context]
  (let [message (:message throw-context)
        cause (:cause throw-context)
        cause-message (c/maybe-nil #(.getMessage %) "" cause)]
    (assoc map :message message :cause cause-message)))

;; more suitable exception info support
(defn ex-info-support
  [handler]
  (fn [request]
    (ss/try+
      (handler request)
      (catch :http-response {:keys [http-response] :as e}
        (http-response (assoc-throw-context (dissoc e :http-response) &throw-context)))
      (catch map? e
        (r/internal-server-error (assoc-throw-context e &throw-context))))))

(alter-var-root #'cm/ex-info-support (constantly ex-info-support))