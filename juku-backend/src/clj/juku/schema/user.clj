(ns juku.schema.user
  (:require [schema.core :as s]))

(s/defschema DbUser {:tunnus  s/Str
                     :etunimi s/Str
                     :sukunimi s/Str
                     (s/optional-key :nimi) s/Str
                     :organisaatioid s/Num
                     :jarjestelma s/Bool})

(s/defschema User (assoc DbUser :roles [s/Str]))