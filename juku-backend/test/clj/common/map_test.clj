(ns common.map-test
  (:require [midje.sweet :refer :all]
            [common.map :as m]
            [clojure.string :as str]))

(facts "Tests for removing map values"
   (fact (m/dissoc-if {:a "1"} str/blank?) => {:a "1"} )
   (fact (m/dissoc-if {:a "1" :b nil :c ""} str/blank?) => {:a "1"} ))

(facts "Tests for removing nil values from a map"
  (fact (m/dissoc-if-nil {:a 1}) => {:a 1})
  (fact (m/dissoc-if-nil {:a 1 :b nil}) => {:a 1}))