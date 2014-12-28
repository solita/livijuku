(ns juku.db.oracle-metrics-test
  (:require [midje.sweet :refer :all]
            [juku.db.oracle-metrics :as m]))

(defn foo [a] (assoc m/*module* :result (+ a 1)))
(m/trace-fn 'foo)
(fact (foo 1) => {:name "juku.db.oracle-metrics-test" :action "foo" :result 2})

(defn bar "docs" ;; line 9
  ([a] (assoc m/*module* :result (+ a 1)))
  ([a b] (assoc m/*module* :result (+ a b))))
(m/trace-fn 'bar)

(facts
  (fact (:arglists (meta #'bar)) => '([a] [a b]))
  (fact (:line (meta #'bar)) => 9)
  (fact (:doc (meta #'bar)) => "docs")
  (fact (bar 1 1) => {:name "juku.db.oracle-metrics-test" :action "bar" :result 2}))

(ns juku.db.oracle-metrics-test.very.long.namespace.name
  (:require [midje.sweet :refer :all]
            [juku.db.oracle-metrics :as m]))
(defn foo [a] (assoc m/*module* :result (+ a 1)))
(m/trace-fn 'foo)
(fact (foo 1) => {:name "name" :action "foo" :result 2})

(ns juku.db.oracle-metrics-test.namespaces
  (:require [midje.sweet :refer :all]
            [juku.db.oracle-metrics :as m]))
(defn foo [a] (assoc m/*module* :result (+ a 1)))
(defn bar [a] (assoc m/*module* :result (+ a 1)))
(m/trace-ns *ns* (constantly true))
(fact (foo 1) => {:name "juku.db.oracle-metrics-test.namespaces" :action "foo" :result 2})
(fact (bar 1) => {:name "juku.db.oracle-metrics-test.namespaces" :action "bar" :result 2})

