(ns juku.schema.user
  (:require [schema.core :as s]))

(s/defschema User {:tunnus  s/Str
                   :roles [s/Str]})