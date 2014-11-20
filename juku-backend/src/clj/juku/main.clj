(ns juku.main
  (:gen-class))

(defn -main [& args]
  (require 'juku.server)
  ((resolve 'juku.server/start)))
