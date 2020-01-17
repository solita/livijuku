(ns juku.service.pdf-mock
  (:require [midje.sweet :refer :all]
            [clj-time.core :as time]
            [clj-time.format :as timef]
            [juku.service.pdf2 :as pdf]
            [clojure.string :as str]
            [common.core :as c]
            [common.string :as strx])
  (:import (java.io InputStream)
           (org.apache.pdfbox.pdfparser PDFParser)
           (org.apache.pdfbox.pdmodel PDDocument)
           (org.apache.pdfbox.text PDFTextStripper)
           (org.apache.pdfbox.io RandomAccessBuffer)))

(def ^:dynamic *mock-pdf*)

(def original-muodosta-pdf pdf/pdf->inputstream)

(defn muodosta-pdf-mock [title date diaarinumero footer content]
  (set! *mock-pdf* (c/bindings->map title date diaarinumero footer content))
  (original-muodosta-pdf title date diaarinumero footer content))

(defmacro with-mock-pdf [& body]
  `(with-redefs [pdf/pdf->inputstream muodosta-pdf-mock]
     (binding [*mock-pdf* {}] ~@body)))

(def today (timef/unparse-local-date (timef/formatter "d.M.yyyy") (time/today)))

(defn assert-header
  ([title diaarinumero] (assert-header title today diaarinumero))
  ([title date diaarinumero]
    (fact "Header is valid"
          (:title *mock-pdf*) => title
          (:date *mock-pdf*) => date
          (:diaarinumero *mock-pdf*) => diaarinumero )))

(defn assert-footer [footer]
  (fact "Footer is valid"
        (:footer *mock-pdf*) => (partial strx/substring? footer)))

(defn pdf->text [^InputStream document]
  (let [^PDFParser parser (doto (PDFParser. (RandomAccessBuffer. document)) .parse)
        ^PDFTextStripper stripper (PDFTextStripper. )]

    (with-open [^PDDocument pdf (.getPDDocument parser)]
      (str/replace (.getText stripper pdf) #"\s+" " "))))