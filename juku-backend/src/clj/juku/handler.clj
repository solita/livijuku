(ns juku.handler
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]

            [juku.reitit :refer [juku-api]]
            [ring.middleware.defaults :refer :all]))


(def app (wrap-defaults #'juku-api (assoc-in site-defaults [:security :anti-forgery] false)))




