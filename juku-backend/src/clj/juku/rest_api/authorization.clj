(ns juku.rest-api.authorization
  (require [juku.service.user :as user]
           [compojure.api.meta :as meta]
           [ring.util.http-response :as http]))

;-- Authorisaatio --
(defmethod meta/restructure-param :auth [_ auth {:keys [body] :as acc}]
  (assoc acc :body `((if (some user/has-privilege* ~auth)
                       (do ~@body)
                       (http/forbidden (str "Käyttäjällä " (:tunnus user/*current-user*)
                                       " ei ole mitään seuraavista vaadituista käyttöoikeuksista: " ~auth))))))