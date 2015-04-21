(ns juku.server
  (:require [org.httpkit.server :as http-kit]
            [clojure.string :as str]
            [juku.handler :as handler]
            [clojure.tools.logging :as log]
            [juku.settings :refer [settings]]))

;;
;; Server life-cycle:
;;

(defonce server (atom nil))

(defn stop-server []
  (if-let [s (deref server)] (s))
  (reset! server nil))

(defn start-server [handler port]
  (stop-server)
  (log/info (str "Starting web server on port " port))
  (reset! server (http-kit/run-server handler {:port port :max-body 209715200})))

(defn start []
  (let [env-port (System/getenv "SERVER_PORT")
        port (if (str/blank? env-port) (get-in settings [:server :port]) (Integer/parseInt env-port))]
    (start-server #'handler/app port)))

(defn stop [] (stop-server))


