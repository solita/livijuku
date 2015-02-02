(ns common.string
  (:require [clojure.string :as str]))

(defn substring? [^CharSequence sub ^String str]
  (.contains str sub))

(defn substring [^CharSequence str start end]
  (.subSequence str start, end))

(defn trim [^CharSequence str remove]
  (substring str remove (- (count str) remove)))

(defn interpolate [template values]
  (str/replace template #"\{.*?\}" (fn [key] (str ((keyword (trim key 1)) values)))))

