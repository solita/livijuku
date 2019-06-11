(ns juku.service.asiahallinta-mock
  (:require [juku.user :as user]
            [clj-time.core :as time]
            [clj-time.format :as tf]
            [common.map :as m]
            [common.collection :as coll]
            [clj-http.fake :as fake]
            [juku.settings :refer [settings]]))

(def ^:dynamic *asha*)

(def default-settings {
  :url "http://asha.livijuku.solita.fi/api"
  :user "test"
  :password "test"
  ;; asioiden omistavan henkilön käyttäjätunnus
  :omistavahenkilo "test"})

(defn asha-handler [operation result] (fn [req] (set! *asha* (assoc *asha* operation req)) {:status 200 :headers {} :body result}))

(defn headers [operation] (get-in *asha* [operation :headers]))

(defn request [operation] (operation *asha*))

(defn valid-headers? [headers]
  (let [eq (fn [header expected-value] (= (get headers header) expected-value))
        not-nil (fn [header] (not (nil? (get headers header))))]

    (and (eq "SOA-KayttajanID" user/*current-user-id*)
         (eq "SOA-Kutsuja" "JUKU")
         (eq "SOA-Kohde" "ASHA")
         (not-nil "SOA-ViestiID")
         (not-nil "SOA-Toiminto")
         (time/before? (tf/parse (tf/formatters :date-time) (get headers "SOA-Aikaleima")) (time/now)))))

(defmacro with-asha [& body]
  `(fake/with-fake-routes
     {#"http://(.+)/hakemuskausi" (asha-handler :avaus "testing\n")
      #"http://(.+)/hakemuskausi/(.+)/sulje" (asha-handler :sulkeminen "")
      #"http://(.+)/hakemuskausi/(.+)/hakuohje" (asha-handler :hakuohje "")

      #"http://(.+)/hakemus" (asha-handler :vireille "testing\n")
      #"http://(.+)/hakemus/(.+)/taydennyspyynto" (asha-handler :taydennyspyynto "")
      #"http://(.+)/hakemus/(.+)/taydennys" (asha-handler :taydennys "")
      #"http://(.+)/hakemus/(.+)/tarkastettu" (asha-handler :tarkastettu "")
      #"http://(.+)/hakemus/(.+)/kasittely" (asha-handler :kasittely "")
      #"http://(.+)/hakemus/(.+)/paatos" (asha-handler :paatos "")

      #"http://(.+)/hakemus/(.+)/maksatushakemus" (asha-handler :maksatushakemus "")}

      (with-redefs [settings (assoc settings :asiahallinta default-settings)]
        (binding [*asha* {}] ~@body))))

(defmacro with-asha-off [& body] `(with-redefs [settings (assoc settings :asiahallinta "off")] ~@body))

(defn group-by-multiparts [request]
  (m/map-values first (group-by (coll/or* :part-name :name) (:multipart request))))