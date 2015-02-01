(ns common.core)

(defn maybe-nil [f default maybe-nil]
  (if (= maybe-nil nil) default (f maybe-nil)))
