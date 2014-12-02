(ns juku.schema.osasto
  (:require [schema.core :as s]))

(s/defschema Osasto {:id     s/Num
                     :nimi   s/Str})




