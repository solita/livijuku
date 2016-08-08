(ns common.core
  (:require [slingshot.slingshot :as ss]
            [clojure.java.io :as io])
  (:import (java.io ByteArrayOutputStream ByteArrayInputStream)))

(defn is-divisible-by [num divisor]
  (zero? (mod num divisor)))

(defn maybe-nil [f default maybe-nil]
  (if (nil? maybe-nil) default (f maybe-nil)))

(defn third [collection] (fnext (next collection)))

(def not-nil? (comp not nil?))

(defn partial-first-arg [f & rest-args] (fn [first-arg] (apply f first-arg rest-args)))

(defn nil-safe [f]
  (fn ([x] (if x (f x)))
      ([x & next] (if (and x (every? not-nil? next)) (apply f x next)))))

(defmacro if-let* [bindings expr else]
  (if (seq bindings)
    `(if-let [~(first bindings) ~(second bindings)]
       (if-let* ~(drop 2 bindings) ~expr ~else) ~else)
    expr))

(defmacro if-let3
  "This macro constructs a hierarchy of nested if-let forms (see if-let).
  The nested if-let structure is created from a list of bindings.
  Here a binding is a triplet of following forms:
  1) a name of the binding,
  2) a value expression of the binding
  3) the default value if the value part is nil.

  The first two forms a normal let binding supporting all the destructing patterns as the normal if-let.

  Examples:
  (if-let3 [a b c] body) => (if-let [a b] body c)
  (if-let3 [a b c, d e f] body) => (if-let [a b] (if-let [d e] body f) c)"

  [bindings expr]
  {:pre [(is-divisible-by (count bindings) 3)]}
  (if (seq bindings)
    `(if-let [~(first bindings) ~(second bindings)]
       (if-let3 ~(drop 3 bindings) ~expr) ~(third bindings))
    expr))

(defmacro if-let3!
  "This is same as if-let3 except instead of a default value an exception is thrown."
  [bindings expr]
  {:pre [(is-divisible-by (count bindings) 3)]}
  (if (seq bindings)
    `(if-let [~(first bindings) ~(second bindings)]
       (if-let3! ~(drop 3 bindings) ~expr) (ss/throw+ ~(third bindings)))
    expr))

(defn nil-if [peridicate value] (if (peridicate value) nil value))

(defmacro error-let
  [bindings body]
  {:pre [(vector? bindings)
         (is-divisible-by (count bindings) 4)]}
  (if (not (empty? bindings))
    (let [form (bindings 0) value (bindings 1) error? (bindings 2) error (bindings 3)]
      `(let [temp# ~value]
         (if (~error? temp#)
           ~error
           (let [~form temp#] (error-let ~(vec (drop 4 bindings)) ~body)))))
    body))

(defmacro error-let!
  [bindings body]
  {:pre [(vector? bindings)
         (is-divisible-by (count bindings) 4)]}
  (if (not (empty? bindings))
    (let [form (bindings 0) value (bindings 1) error? (bindings 2) error (bindings 3)]
      `(let [temp# ~value]
         (if (~error? temp#)
           (ss/throw+ ~error)
           (let [~form temp#] (error-let! ~(vec (drop 4 bindings)) ~body)))))
    body))

(defn slurp-bytes
  "Slurp the bytes from a slurpable thing"
  [input]
  (with-open [^ByteArrayOutputStream out (ByteArrayOutputStream.)]
    (io/copy (io/input-stream input) out)
    (.toByteArray out)))

(defn output->input-stream [f]
  (with-open [output (ByteArrayOutputStream.)]
    (f output)
    (ByteArrayInputStream. (.toByteArray output))))

(defn setup-shutdown-hook! [f]
  (.addShutdownHook (Runtime/getRuntime) (Thread. f)))

(defmacro bindings->map [& bindings]
    (into {} (map (fn [s] [(keyword (name s)) s]) bindings)))

(defn cartesian-product [colls]
  (if (empty? colls)
    '(())
    (for [x (first colls)
          more (cartesian-product (rest colls))]
      (cons x more))))

(defn assert-not-nil! [value error]
  (if (nil? value)
    (ss/throw+ error)
    value))