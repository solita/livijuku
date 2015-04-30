(ns juku.headers
  (:require [ring.util.codec :as codec]
            [common.core :as c])
  (:import (java.nio.charset Charset)))

;; Header-arvojen base64-enkoodaukseen käytetty muoto
(def base64-header-form #"=\?UTF-8\?B\?(.+)\?=")

(defn parse-header
  "Lue mahdollisesti base64-koodatun otsikkotiedon arvo.
  Jos otsikkoa ei tulkita oikealla tavalla base64-koodatuksi, niin otsikon arvo palautetaan sellaisenaan."

   ([headers header not-found] {:pre [(map? headers) (c/not-nil? header)]}

      (if-let [value (get headers header)]
        (if-let [base64-value (re-find base64-header-form value)]
          (String. (codec/base64-decode (second base64-value)) (Charset/forName "utf-8")) value)
        not-found))

   ([headers header] (parse-header headers header nil)))
