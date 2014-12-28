(ns juku.db.oracle-metrics
  (:import (oracle.jdbc OracleConnection))
  (:require [clojure.string :as str]))

(def end-to-end-clientid-length 64)
(def end-to-end-module-length 48)
(def end-to-end-action-length 32)

(defn- truncate-string
  [string max-length]
  (when-not (str/blank? string)
    (.substring string 0 (min (.length string) max-length))))

(defn- build-null-metrics []
  (into-array String (vec (repeat OracleConnection/END_TO_END_STATE_INDEX_MAX nil))))

(defn- build-metrics
  [client-id action module]
  (into-array String (assoc (vec (repeat OracleConnection/END_TO_END_STATE_INDEX_MAX nil))
                     OracleConnection/END_TO_END_CLIENTID_INDEX (truncate-string client-id end-to-end-clientid-length)
                     OracleConnection/END_TO_END_ACTION_INDEX (truncate-string action end-to-end-action-length)
                     OracleConnection/END_TO_END_MODULE_INDEX (truncate-string module end-to-end-module-length))))

(defn- set-end-to-end-metrics-ora [^OracleConnection ora-connection metrics]
  (.setEndToEndMetrics ora-connection metrics (short 0)))

(defn set-end-to-end-metrics
  [connection client-id module action]
  (let [^OracleConnection ora-connection (.unwrap connection OracleConnection)]
    (set-end-to-end-metrics-ora ora-connection (build-null-metrics))
    (set-end-to-end-metrics-ora ora-connection (build-metrics client-id action module))))

;; function tracing for end2end metrics module/action information ;;

(def ^:dynamic *module*)

(defn with-module*
  [name action f]
  (binding [*module* {:name name :action action}] (f)))

(defmacro with-module [name action & body]
  `(with-module* ~name ~action (fn [] ~@body)))

(defn trace-fn [function]
  (let [var (if (var? function) function (resolve function))
        metadata (meta var)
        fn-name (name (:name metadata))
        namespace-name (-> (:ns metadata) ns-name name)
        module-name (if (> (count namespace-name) end-to-end-module-length)
                      (last (str/split namespace-name #"\."))
                      namespace-name)]
    (alter-var-root var
        (fn [original-function]
          (fn [& args] (with-module module-name fn-name (apply original-function args)))))))

(defn trace-ns [namespace metadata-filter]
  (doseq [function (filter (comp metadata-filter meta) (filter (comp fn? var-get) (vals (ns-publics namespace))))]
    (trace-fn function)))
