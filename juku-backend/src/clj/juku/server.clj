(ns juku.server
  (:require [org.httpkit.server :as http-kit]
            [clojure.string :as str]
            [common.core :as c]
            [juku.handler :as handler]
            [clojure.tools.logging :as log]
            [juku.settings :refer [settings]]))

;;
;; HTTP-kit server life-cycle:
;; http://www.http-kit.org/server.html
;;

(defonce server (atom nil))

(defn stop-server []
  (when-not (nil? @server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (log/info "Stopping web server")
    (@server :timeout 100)
    (reset! server nil)))

(defn start-server [handler port]
  (stop-server)
  (log/info (str "Environment: " (System/getenv)))
  (log/info (str "System properties: " (System/getProperties)))
  (log/info (str "Starting web server on port " port))
  (reset! server (http-kit/run-server handler
     {:port port
      :max-line 65536
      :max-body 209715200
      :thread 20})))

(defn start []
  (let [env-port (System/getenv "SERVER_PORT")
        port (if (str/blank? env-port) (get-in settings [:server :port]) (Integer/parseInt env-port))]
    (start-server #'handler/app port)))

(defn stop [] (stop-server))

(c/setup-shutdown-hook! stop)

