(ns juku.schema.organisaatio
  (:require [schema.core :as s]))

(s/defschema Organisaatio {:id     s/Num
                           :nimi   s/Str
                           :lajitunnus   s/Str})




