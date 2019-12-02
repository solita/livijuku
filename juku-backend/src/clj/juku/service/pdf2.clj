(ns juku.service.pdf2
  (:require [clj-pdf.core :as pdf]
            [clj-pdf.utils :as pdf-utils]
            [clj-pdf-markdown.core :as md]
            [clojure.java.io :as io])
  (:import (com.lowagie.text.pdf PdfWriter PdfReader PdfTemplate PdfContentByte BaseFont)
           (com.lowagie.text HeaderFooter Phrase FontFactory Font Document Rectangle)
           (org.commonmark.ext.gfm.tables TablesExtension)
           (org.commonmark.parser Parser)
           (org.commonmark.node Node)))

(def default-font
  {:size 10
   :encoding :unicode
   :ttf-name "pdf-sisalto/roboto/Roboto-Regular.ttf"})

(def margin
  {:letterhead-top    25
   :page-number-top   20
   :page-number-right 60
   :top               36
   :bottom            36
   :right             36
   :left              36})

(def ^BaseFont base-font (.getBaseFont (pdf-utils/font default-font)))

(defn add-logo
  [^Rectangle position ^PdfContentByte canvas]
  (with-open [pdf (io/input-stream (io/resource "pdf-sisalto/traficom-logo.pdf"))
              logo (PdfReader. pdf)]
    (let [writer (.getPdfWriter canvas)
          logo-page (.getImportedPage writer logo 1)]
      (.addTemplate canvas logo-page 0.25 0 0 0.25 (.getLeft position) (.getBottom position) ))))

(defn add-header [page-count-template footer]
  (fn [^PdfWriter writer ^Document document]
    (let [canvas (.getDirectContent writer)
          current-page (.getPageNumber writer)
          length (.getWidthPoint base-font (str current-page " ") (float 10.0))
          page-size (.getPageSize document)
          x (.getRight page-size (:page-number-right margin))
          y (.getTop page-size (+ (:page-number-top margin) 10))]

      (doto canvas
        .beginText
        (.setTextMatrix x y)
        (.setFontAndSize base-font 10)
        (.showText (str current-page " "))
        .endText
        (.addTemplate @page-count-template (+ x length) (- y 5)))
      (when (= current-page 1)
        (.setMargins document
                     (.leftMargin document)
                     (.rightMargin document)
                     (:top margin) ;; space for header
                     (.bottomMargin document))))))

(defn add-footer [^Document document footer]
  (.setFooter document
              (doto (HeaderFooter. (Phrase. footer (pdf-utils/font default-font)) false)
                (.setBorder 1)
                (.setAlignment 0))))

(defn create-page-count-template [^PdfWriter writer page-count-template]
  (reset! page-count-template (PdfTemplate/createTemplate writer 30 20)))

(defn add-page-count [page-count-template]
  (fn [^PdfWriter writer _]
    (doto @page-count-template
      .beginText
      (.setFontAndSize base-font 10)
      (.setTextMatrix 0 5)
      (.showText (str "(" (dec (.getPageNumber writer)) ")"))
      .endText)))

(defn metadata [title date diaarinro footer]
  (let [page-count-template (atom nil)]
    {:title  title
     :subject "Test s"
     :author "Liikenne- ja viestintävirasto"
     :creator "Liikenne- ja viestintävirasto"
     :orientation :portrait
     :top-margin (:letterhead-top margin)
     :bottom-margin (:bottom margin)
     :right-margin (:right margin)
     :left-margin (:left margin)
     :size :a4
     :on-document-open
     (fn [^PdfWriter writer ^Document document]
       (create-page-count-template writer page-count-template)
       #_(add-footer document footer))
     :on-document-close (add-page-count page-count-template)
     :on-page-start (add-header page-count-template footer)
     :letterhead [
      [:pdf-table
         {:border false :width 200 :title "otsikko" }
         [20 10 10]
         [[:pdf-cell {:base-layer-fn add-logo :height 10 :rowspan 3} ""]
          [:pdf-cell {:padding [0 0 0 0] :colspan 2} title]
          [:pdf-cell {:padding [0 0 0 0]} [:phrase {:style :bold} "Päiväys/Datum"]]]
         [[:pdf-cell {:padding [0 0 0 0]} [:phrase {:style :bold} "Dnro/Dnr"]]]
         [[:pdf-cell {:padding [0 0 0 0]} date] [:pdf-cell {:padding [0 0 0 0]} diaarinro]]]]
     :font default-font
     :footer {:text footer
              :align :left
              :page-numbers false}
     :pages false}))

(defn parse-markdown [s]
  (let [^Parser parser (.build (.extensions (Parser/builder) [(TablesExtension/create)]))]
    (.parse parser s)))

(defn process-header [pdf-config ^Node node]
  (if (instance? org.commonmark.ext.gfm.tables.TableHead node)
    {:header (get (md/render-children* pdf-config node) 0)}
    {}))

(defmethod md/render :org.commonmark.ext.gfm.tables.TableBlock [pdf-config ^Node node]
  (into [:table (merge (process-header pdf-config (.getFirstChild node))
                       (get-in pdf-config [:table] {}))]
        (md/render-children* pdf-config (.getLastChild node))))

(defmethod md/render :org.commonmark.ext.gfm.tables.TableRow [pdf-config ^Node node]
  (md/render-children* pdf-config node))

(defmethod md/render :org.commonmark.ext.gfm.tables.TableCell [pdf-config ^Node node]
  (into [:cell] (md/render-children* pdf-config node)))

(def markdown-defaults
 {:table {:border false :padding -2}})

(defn pdf [title date diaari-number footer content out]
  (pdf/pdf [
    (metadata title date diaari-number footer)
    (with-redefs [md/parse-markdown parse-markdown]
      (md/markdown->clj-pdf markdown-defaults content))] out))

(pdf "Avustushakemus" "1.1.2020" "1234832498SD" "Testing"
  "# Testing

| | | |
| -------- | -------- | -------- |
| John     | Doe      | Male     |
| Mary     | Smith    | Female   |



asdf asdf" "test.pdf")