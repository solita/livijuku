(ns juku.middleware
  (:require [juku.user :as current-user]
            [juku.service.user :as user]
            [common.map :as m]
            [common.xforms :as f]
            [clojure.string :as str]
            [ring.util.http-response :as r]))

(defn wrap-user [handler]
  (fn [request]
    (f/if-let* [headers (m/keys-to-keywords (:headers request))
                uid (:oam-remote-user headers)]
      (current-user/with-user-id uid
        (f/if-let* [group-txt (:oam-groups headers)
                    roles (str/split group-txt #",")
                    user (user/find-user uid)]
               (user/with-user (assoc user :roles roles) (handler request))
               (r/forbidden (str "Käyttäjällä " uid " ei ole voimassaolevaa käyttöoikeutta järjestelmään."))))
      (r/forbidden "Käyttäjätunnusta ei löydy pyynnön otsikkotiedosta: oam-remote-user."))))
