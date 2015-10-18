(ns midje.junit
  (:use midje.emission.util)
  (:require [midje.data.fact :as fact]
            [midje.config :as config]
            [midje.emission.state :as state]
            [midje.emission.plugins.util :as util]
            [midje.emission.plugins.silence :as silence]
            [midje.emission.plugins.default-failure-lines :as lines]
            [clj-time.core :as time]
            [clojure.string :as str]
            [clojure.xml :as xml]))

;; This plugin requires all emission api calls to be
;; forwarded to it.
(config/change-defaults :print-level :print-facts)

(def report-file "target/report.xml")

(defn log [text] (spit report-file text :append true))

(defn log-xml [xml-element]
  (log (with-out-str (xml/emit-element xml-element))))

(defn reset-log [] (spit report-file ""))

(def fact-stack (atom []))
(def start-time (atom nil))

(defn- fact-name [fact]
  (or (fact/name fact)
      (fact/description fact)
      (str (fact/file fact) ":" (fact/line fact))))

(defn pass []
  (log-xml (assoc-in (peek @fact-stack)
                     [:attrs :time]
                     (time/in-seconds (time/interval @start-time (time/now)))))
  #_(reset! start-time (time/now)))

(defn testcase-with-failure [failure-map]
  (let [testcase (peek @fact-stack)
        failure-content (str "<![CDATA[" (apply str (lines/summarize failure-map)) "]]>")
        fail-type (:type failure-map)
        fail-element {:tag :failure
                      :content [failure-content]
                      :attrs {:type fail-type}}
        testcase-with-failure (assoc testcase :content [fail-element])]
    testcase-with-failure))

(defn escape [s]
  (if s
    (str/escape s {\" "&quot;"
                   \' "&apos;"
                   \< "&lt;"
                   \> "&gt;"
                   \& "&amp;"})
    ""))

(defn fail [failure-map]
  (log-xml (testcase-with-failure failure-map)))

(defn push [fact-stack fact]
  (let [fact-namespace (or
                         (get-in (peek fact-stack) [:attrs :classname])
                         (str (fact/namespace fact)))

        fact-name (str/join "/" (conj (vec (map (comp :name :attrs) fact-stack))
                                      (fact-name fact)))]

    (conj fact-stack {:tag :testcase
                      :attrs {:classname (escape fact-namespace) :name (escape fact-name)}})))

(defn starting-to-check-fact [fact]
  (when (empty? @fact-stack) (reset! start-time (time/now)))
  (swap! fact-stack push fact))

(defn finishing-fact [fact]
  (swap! fact-stack pop))

(defn starting-fact-stream []
  (reset-log)
  (log "<testsuite>"))

(defn finishing-fact-stream [midje-counters clojure-test-map]
  (log "</testsuite>"))

(def emission-map (merge silence/emission-map
                         { :fail fail
                           :pass pass
                           :starting-fact-stream starting-fact-stream
                           :finishing-fact-stream finishing-fact-stream
                           :starting-to-check-fact starting-to-check-fact
                           :finishing-fact finishing-fact}))

(state/install-emission-map emission-map)