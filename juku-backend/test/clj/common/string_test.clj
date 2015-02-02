(ns common.string-test
  (:require [midje.sweet :refer :all]
            [common.string :as strx]))

(facts
  "Interpolation tests"
  (fact (strx/interpolate "test {a}" {:a "b"}) => "test b")
  (fact (strx/interpolate "test {a}" {:a 1}) => "test 1")
  (fact (strx/interpolate "test {a} - {b}" {:a 1 :b 2}) => "test 1 - 2")
  )