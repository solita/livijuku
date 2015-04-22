(ns common.string
  (:require [clojure.string :as str]
            [common.core :as c]))

(defn substring? [^CharSequence sub ^String str]
  (.contains str sub))

(defn substring
  ([^CharSequence str start end] (.subSequence str start, end))
  ([^CharSequence str remove] (substring str remove (- (count str) remove))))

(defn interpolate [template values]
  (str/replace template #"\{.*?\}" (fn [key] (str ((keyword (substring key 1)) values)))))

(defn trim [^CharSequence txt] (if (c/not-nil? txt) (str/trim txt)))

