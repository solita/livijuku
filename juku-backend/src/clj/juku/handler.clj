(ns juku.handler
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.middleware.resource :refer [wrap-resource]]

            [juku.reitit :refer [reitit]]
            [ring.middleware.defaults :refer :all]))


(def app (wrap-defaults #'reitit (assoc-in site-defaults [:security :anti-forgery] false)))




