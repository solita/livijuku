(ns juku.service.asiahallinta-mock
  (:require [juku.user :as user]
            [clj-time.core :as time]
            [clj-time.format :as tf]
            [juku.settings :refer [settings]]))

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

(defmacro with-asha [& body]
  `(fake/with-fake-routes {#"http://(.+)/hakemuskausi" (asha/asha-handler :avaus "testing\n")
                           #"http://(.+)/hakemuskausi/(.+)/sulje" (asha/asha-handler :sulkeminen "")

                           #"http://(.+)/hakemus" (asha/asha-handler :vireille "testing\n")
                           #"http://(.+)/hakemus/(.+)/taydennyspyynto" (asha/asha-handler :taydennyspyynto "")
                           #"http://(.+)/hakemus/(.+)/taydennys" (asha/asha-handler :taydennys "")
                           #"http://(.+)/hakemus/(.+)/tarkastettu" (asha/asha-handler :tarkastettu "")
                           #"http://(.+)/hakemus/(.+)/kasittely" (asha/asha-handler :kasittely "")}
      (binding [*asha* {}] ~@body)))

(defmacro with-asha-off [& body] `(with-redefs [settings (assoc settings :asiahallinta "off")] ~@body))