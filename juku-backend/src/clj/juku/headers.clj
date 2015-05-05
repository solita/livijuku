(ns juku.headers
  "This module contains encode/decode functions for a standard: rfc2047 - https://www.ietf.org/rfc/rfc2047.txt.
   Used character encoding is utf-8 and binary encoding is base64.
   Encoded form is =?UTF-8?B?base64-encoded-txt?= and it is used only if the original value contains non us-ascii characters."

  (:require [ring.util.codec :as codec]
            [common.core :as c]))

;; Header-arvojen base64-enkoodaukseen käytetty muoto
(def base64-header-form #"=\?UTF-8\?B\?(.+)\?=")

(defn contains-non-us-ascii-text? [txt]
  (if (c/not-nil? txt) (c/not-nil? (re-find #"[^\x00-\x7f]" txt)) false))

(defn decode-value [value]
  (if-let [base64-value (re-find base64-header-form value)]
    (String. (codec/base64-decode (second base64-value)) "utf-8") value))

(defn encode-value [value]
  (if (contains-non-us-ascii-text? value)
    (str "=?UTF-8?B?" (codec/base64-encode (.getBytes value "utf-8")) "?=")
    value))

(defn parse-header
  "Lue mahdollisesti base64-koodatun otsikkotiedon arvo.
  Jos otsikkoa ei tulkita oikealla tavalla base64-koodatuksi, niin otsikon arvo palautetaan sellaisenaan."

   ([headers header not-found] {:pre [(map? headers) (c/not-nil? header)]}
      (if-let [value (get headers header)] (decode-value value) not-found))

   ([headers header] (parse-header headers header nil)))

