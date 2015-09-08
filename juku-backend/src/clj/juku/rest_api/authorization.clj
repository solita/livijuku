(ns juku.rest-api.authorization
  (require [clojure.string :as s]
           [juku.service.user :as user]
           [compojure.api.meta :as meta]
           [juku.headers :as h]
           [clojure.tools.logging :as log]
           [ring.util.http-response :as http]
           [common.core :as c]))

;-- Authorisaatio --
(defmethod meta/restructure-param :auth [_ auth {:keys [body] :as acc}]
  (assoc acc :body `((if (some user/has-privilege* ~auth)
                       (do ~@body)
                       (let [msg# (str "Käyttäjällä " (:tunnus user/*current-user*) " ei ole vaadittua käyttöoikeutta. "
                                      "Käyttäjällä pitää olla vähintään yksi seuraavista käyttöoikeuksista: " ~auth)]
                         (log/error msg#)
                         (h/content-type-text-plain (http/forbidden msg#)))))))

; -- Auditing --
(defmethod meta/restructure-param :audit [_ data {:keys [body] :as acc}]
  (assoc acc :body `((log/log "juku.rest-api.audit" :info nil
                              (str (name (:request-method ~meta/+compojure-api-request+)) " "
                                   (:uri ~meta/+compojure-api-request+) " - "
                                   (:tunnus user/*current-user*)
                                   (if (empty? ~data)
                                     ""
                                     (str " - " (s/join " - "(map (comp pr-str ~meta/+compojure-api-request+) ~data))))))
                      ~@body)))