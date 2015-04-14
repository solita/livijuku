(ns juku.service.asiahallinta
  (:require [juku.user :as user]))

(def ^:dynamic *asha*)

(defn asha-handler [operation result] (fn [req] (set! *asha* (assoc *asha* operation req)) {:status 200 :headers {} :body result}))

(defn headers [operation] (get-in *asha* [operation :headers]))

(defn request [operation] (operation *asha*))

(defn valid-headers? [headers]
  (= (get headers "SOA-KayttajanID") user/*current-user-id*))

(defmacro with-asha [& body] `(binding [*asha* {}] ~@body))