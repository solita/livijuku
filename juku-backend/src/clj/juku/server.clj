(ns juku.server
  (:require [org.httpkit.server :as hs]
            [juku.handler :as handler]))

(defn start []
  (let [port (Integer/parseInt (or (System/getenv "JUKU_PORT")
                                   "8082"))]
    (println "Running server on port " port)
    (hs/run-server handler/app {:port port})))
