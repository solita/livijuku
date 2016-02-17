(ns common.settings
  (:require [clojure.java.io :refer [file]]
            [clojure.tools.logging :as log]
            [common.map :as cm]
            [schema.coerce :as sc])
  (:import (java.util Properties)
           (java.io FileNotFoundException)))

(defn string->boolean [x]
  (case x
    "true" true
    "false" false
    x))

(defn string-coercion-matcher [schema]
  (or (sc/string-coercion-matcher schema)
      ({Boolean string->boolean} schema)))

(defn coerce-settings [settings type]
  ((sc/coercer! type string-coercion-matcher) settings))

(defn read-settings-from-file [file]
  (try
    (with-open [reader (clojure.java.io/reader file)]
      (let [properties (doto (Properties.) (.load reader))]
        (into {} (for [[k v] properties] [(keyword k) v]))))
    (catch FileNotFoundException _
      (log/info "Asetustiedostoa ei löydy. Käytetään oletusasetuksia")
      {})))

(defn read-settings
  ([file schema] (read-settings file {} schema))
  ([file defaults schema]
  (let [settings (cm/flat->tree (read-settings-from-file file) #"\.")
        complete-settings (cm/deep-merge defaults settings)]
    (coerce-settings complete-settings schema))))
