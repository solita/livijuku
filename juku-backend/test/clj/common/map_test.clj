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

(fact "zip object"
      (m/zip-object [] []) => {}
      (m/zip-object [:a] [1]) => {:a 1}
      (m/zip-object [:a :b] [1 2]) => {:a 1 :b 2})

(fact "deep reduce"
      (m/deep-reduce conj [] {:a 1}) => [[:a 1]]
      (m/deep-reduce conj [] {:a 1 :b 1}) => [[:a 1] [:b 1]])

;; TODO: (m/deep-reduce conj [] {:a {:c 1 :d 1} :b 1}) => [[:a 1] [:b 1]]