(ns common.core)

(defn maybe-nil [f default maybe-nil]
  (if (= maybe-nil nil) default (f maybe-nil)))

(defmacro if-let* [bindings expr else]
  (if (seq bindings)
    `(if-let [~(first bindings) ~(second bindings)]
       (if-let* ~(drop 2 bindings) ~expr ~else) ~else)
    expr))
