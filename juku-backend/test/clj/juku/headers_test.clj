(ns juku.headers-test
  (:require [midje.sweet :refer :all]
            [juku.headers :as h]))

(defn- parse [value]
  (h/parse-header {:test value} :test))

(facts "Normal string header values"
   (fact (parse "testi") => "testi")
   (fact (parse "") => "")
   (fact (parse nil) => nil))

(facts "Base64 encoded header values - form: =?UTF-8?B?(.+)?="
   (fact (parse "P%C3%A4k%C3%A4") => "Päkä")
   (fact (parse "P%C3%A4%C3%A4k%C3%A4ytt%C3%A4j%C3%A4") => "Pääkäyttäjä"))

(facts "Decode values"
   (fact (h/encode-value nil) => nil)
   (fact (h/encode-value "") => "")
   (fact (h/encode-value "test") => "test")
   (fact (h/encode-value "ääkköset") => "=?UTF-8?B?w6TDpGtrw7ZzZXQ=?="))