(ns juku.user)

(def ^:dynamic *current-user-id* "JUKU_APP")

(defn with-user-id*
  [user-id f]
  (binding [*current-user-id* user-id] (f)))

(defmacro with-user-id [user-id & body]
  `(with-user-id* ~user-id (fn [] ~@body)))