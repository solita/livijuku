(ns juku.handler
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]

            [juku.reitit :refer [juku-api]]
            [juku.middleware :as m]
            [ring.middleware.defaults :refer :all]))


(def app (-> #'juku-api
  m/wrap-user
  (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))))









