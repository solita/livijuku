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

(defn start-with [^String txt ^String prefix] (.startsWith txt prefix))

(defn not-blank? [txt] (and (c/not-nil? txt) (not (empty? txt))))

(defn blank-if-nil [prefix value] (if (nil? value) "" (str prefix value)))

