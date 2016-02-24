(ns juku.service.email-mock
  (:require [midje.sweet :refer :all]
            [juku.service.email :as email]))

(def ^:dynamic *mock-email*)

(def original-post email/post)

(defn post-mock
  ([to subject body]
    (set! *mock-email* (conj *mock-email* {:to to :subject subject :body body}))
    (original-post to subject body))
  ([to subject body asiakirja-nimi asiakirja]
   (set! *mock-email* (conj *mock-email* {:to to :subject subject :body body :asiakirja asiakirja-nimi}))
   (original-post to subject body asiakirja-nimi asiakirja)))

(defmacro with-mock-email [& body]
  `(with-redefs [email/post post-mock]
     (binding [*mock-email* []] ~@body)))
