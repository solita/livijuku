(ns juku.rest-api.authorization
  (require [juku.service.user :as user]
           [compojure.api.meta :as meta]
           [clojure.tools.logging :as log]
           [ring.util.http-response :as http]))

;-- Authorisaatio --
(defmethod meta/restructure-param :auth [_ auth {:keys [body] :as acc}]
  (assoc acc :body `((if (some user/has-privilege* ~auth)
                       (do ~@body)
                       (let [msg (str "Käyttäjällä " (:tunnus user/*current-user*) " ei ole vaadittua käyttöoikeutta. "
                                      "Käyttäjällä pitää olla vähintään yksi seuraavista käyttöoikeuksista: " ~auth)]
                         (log/error msg)
                         (http/forbidden msg))))))