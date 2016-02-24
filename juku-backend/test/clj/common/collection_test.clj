(ns common.collection-test
  (:require [midje.sweet :refer :all]
            [common.collection :as c]))

(facts
  "Assoc join tests"
  (fact (c/assoc-join [{:a 1}] :c [] [:a]) => [{:a 1, :c []}] )
  (fact (c/assoc-join [{:a 1}] :c [{:a 1}] [:a]) => [{:a 1, :c [{:a 1}]}] )
  (fact (c/assoc-join [{:a 1}] :c [{:a 1 :b 1}] [:a]) => [{:a 1, :c [{:a 1 :b 1}]}] )
  (fact (c/assoc-join [{:a 1}] :c [{:a 1 :b 1} {:a 1 :b 2}] [:a]) => [{:a 1, :c [{:a 1 :b 1} {:a 1 :b 2}]}] )
  (fact (c/assoc-join [{:a 1} {:a 2}] :c [{:a 1 :b 1} {:a 2 :b 2}] [:a]) => [{:a 1, :c [{:a 1 :b 1}]}, {:a 2, :c [{:a 2 :b 2}]}] ))

(fact
  "or* tests"
  ((c/or* (constantly 1)) nil) => 1
  ((c/or* :a (constantly 1)) nil) => 1
  ((c/or* :a :b) {:a nil :b 1}) => 1
  ((c/or* :a :b (constantly 1)) {:a nil :b nil}) => 1
  (filter (c/or* (c/eq :a 1) (c/eq :b 1))
          [{:a nil :b nil} {:a 1 :b nil} {:a nil :b 1}]) => [{:a 1 :b nil} {:a nil :b 1}]
  (map (c/or* :a :b (constantly 0))
       [{:a nil :b nil} {:a 1 :b nil} {:a nil :b 2}]) => [0 1 2])