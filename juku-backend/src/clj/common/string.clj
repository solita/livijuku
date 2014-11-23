(ns common.string
  (:require [clojure.string :as str]))

(defn substring? [sub str]
  (.contains str sub))

