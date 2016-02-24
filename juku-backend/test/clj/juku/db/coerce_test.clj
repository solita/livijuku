(ns juku.db.coerce-test
  (:require [midje.sweet :refer :all]
            [juku.db.coerce :as coerce]))

(facts
  "Row mapper tests"
  (fact
    (coerce/row->object {:a 1}) => {:a 1})

  (fact
    (coerce/row->object {:a_x 1 :a_y 2}) => {:a {:x 1 :y 2}})

  (fact
    (coerce/row->object {:a_x 1 :a_y 2 :b 3}) => {:a {:x 1 :y 2} :b 3})

  (fact
    (coerce/row->object {:a_x 1 :a_y 2 :b_x 3 :b_y 4}) => {:a {:x 1 :y 2} :b {:x 3 :y 4}})

  (fact
    (coerce/object->row  {:a {:x 1 :y 2}}) => {:a_x 1 :a_y 2})

  ;; object->row is inverse function of row->object
  (fact
    (let [x {:a_x 1 :a_y 2 :b 3}]
      (coerce/object->row (coerce/row->object x)) => x))
  )
