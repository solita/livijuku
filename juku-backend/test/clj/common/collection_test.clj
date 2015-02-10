(ns common.collection-test
  (:require [midje.sweet :refer :all]
            [common.collection :as c]))

(facts
  "Left join tests"
  (fact (c/assoc-left-join :c [{:a 1}] [] :a) => [{:a 1, :c #{}}] )
  (fact (c/assoc-left-join :c [{:a 1}] [{:a 1}] :a) => [{:a 1, :c #{{:a 1}}}] )
  (fact (c/assoc-left-join :c [{:a 1}] [{:a 1 :b 1}] :a) => [{:a 1, :c #{{:a 1 :b 1}}}] )
  (fact (c/assoc-left-join :c [{:a 1}] [{:a 1 :b 1} {:a 1 :b 2}] :a) => [{:a 1, :c #{{:a 1 :b 1} {:a 1 :b 2}}}] )
  (fact (c/assoc-left-join :c [{:a 1} {:a 2}] [{:a 1 :b 1} {:a 2 :b 2}] :a) => [{:a 1, :c #{{:a 1 :b 1}}}, {:a 2, :c #{{:a 2 :b 2}}}] ))