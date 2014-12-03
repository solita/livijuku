(ns juku.server
  (:require [org.httpkit.server :as http-kit]
            [juku.handler :as handler]))

;;
;; Server life-cycle:
;;

(defonce server (atom nil))

(defn stop-server []
  (if-let [s (deref server)]
    (s))
  (reset! server nil))

(defn start-server [routes port]
  (stop-server)
  (let [port (or port 8082)]
    (println (str "Starting web server on port " port))
    (reset! server (http-kit/run-server routes {:port port}))))

(defn start []
  (let [port (Integer/parseInt (or (System/getenv "JUKU_PORT") "8082"))]
    (start-server #'handler/app port)))

(defn stop []
  (stop-server))


