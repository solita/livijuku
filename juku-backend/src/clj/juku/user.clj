(ns juku.user
  (:require [clojure.string :as str]
            [common.map :as m]))

(def ^:dynamic *current-user*)

(defn user-fullname [user]
  (str (:etunimi user) " " (:sukunimi user)))

(defn with-user*
  [user f]
  (binding [*current-user* user] (f)))

(defmacro with-user [user & body]
  `(with-user* ~user (fn [] ~@body)))

(defn wrap-user [handler]
  (fn [request]
    (let [headers (m/keys-to-keywords (:headers request))
          uid (:oam-remote-user headers)
          group-txt (or (:oam-groups headers) "")
          groups (str/split group-txt #",")]
      (try
        (with-user {:uid uid :groups groups} (handler request))
        (catch IllegalStateException _
          {:headers {"Content-Type" "text/plain;charset=utf-8"}
           :status 403
           :body (str "Käyttäjällä " uid " ei ole voimassaolevaa käyttöoikeutta.")})))))
