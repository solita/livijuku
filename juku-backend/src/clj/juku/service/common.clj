(ns juku.service.common
  (:require [slingshot.slingshot :as ss]))

(defn retry [tries f & args]
  (let [res (try {:value (apply f args)}
                 (catch Exception e
                   (if (= 0 tries)
                     (throw e)
                     {:exception e})))]
    (if (:exception res)
      (recur (dec tries) f args)
      (:value res))))
