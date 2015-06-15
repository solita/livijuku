(ns common.core
  (:require [slingshot.slingshot :as ss]))

(defn is-divisible-by [num divisor]
  (= (mod num divisor) 0))

(defn maybe-nil [f default maybe-nil]
  (if (= maybe-nil nil) default (f maybe-nil)))

(defn third [collection] (first (next (next collection))))

(def not-nil? (comp not nil?))

(defn nil-safe [f]
  (fn ([x] (if x (f x)))
      ([x & next] (if (and x (every? not-nil? next)) (apply f x next)))))

(defmacro if-let* [bindings expr else]
  (if (seq bindings)
    `(if-let [~(first bindings) ~(second bindings)]
       (if-let* ~(drop 2 bindings) ~expr ~else) ~else)
    expr))

(defmacro if-let3 [bindings expr]
  {:pre [(is-divisible-by (count bindings) 3)]}
  (if (seq bindings)
    `(if-let [~(first bindings) ~(second bindings)]
       (if-let3 ~(drop 3 bindings) ~expr) ~(third bindings))
    expr))

(defmacro if-let3! [bindings expr]
  {:pre [(is-divisible-by (count bindings) 3)]}
  (if (seq bindings)
    `(if-let [~(first bindings) ~(second bindings)]
       (if-let3! ~(drop 3 bindings) ~expr) (ss/throw+ ~(third bindings)))
    expr))