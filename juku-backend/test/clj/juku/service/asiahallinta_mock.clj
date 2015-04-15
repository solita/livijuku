(ns juku.service.asiahallinta-mock
  (:require [juku.user :as user]
            [clj-time.core :as time]
            [clj-time.format :as tf]))

(def ^:dynamic *asha*)

(defn asha-handler [operation result] (fn [req] (set! *asha* (assoc *asha* operation req)) {:status 200 :headers {} :body result}))

(defn headers [operation] (get-in *asha* [operation :headers]))

(defn request [operation] (operation *asha*))

(defn valid-headers? [headers]
  (let [eq (fn[header expected-value] (= (get headers header) expected-value))
        not-nil (fn [header] (not (nil? (get headers header))))]

    (and (eq "SOA-KayttajanID" user/*current-user-id*)
         (eq "SOA-Kutsuja" "JUKU")
         (eq "SOA-Kohde" "ASHA")
         (not-nil "SOA-ViestiID")
         (not-nil "SOA-Toiminto")
         (time/before? (tf/parse (tf/formatters :date-time) (get headers "SOA-Aikaleima")) (time/now)))))

(defmacro with-asha [& body] `(binding [*asha* {}] ~@body))