(ns common.core-test
  (:require [midje.sweet :refer :all]
            [common.core :as c]))

(fact
  "if-let* tests"
  (c/if-let* [a 1] a) => 1
  (c/if-let* [a 1 b 2] b) => 2
  (c/if-let* [a 1 b 2 c nil] b, 3) => 3
  (c/if-let* [a 1 b 2 c nil] b) => nil
  (c/if-let* [a 1 b 2 c false] b) => nil
  (c/if-let* [a 1 b (inc a) c (inc b)] c) => 3)

(fact
  "if-let* tests - invalid usage"
  (macroexpand `(c/if-let* [a] a)) => (throws AssertionError))
