(ns common.map)

(defn remove-keys [m keys]
  (apply dissoc (concat [m] keys)))