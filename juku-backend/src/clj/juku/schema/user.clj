(ns juku.schema.user
  (:require [schema.core :as s]))

(s/defschema User {:tunnus  s/Str
                 (s/optional-key :etunimi) s/Str
                 (s/optional-key :sukunimi) s/Str
                 (s/optional-key :nimi) s/Str
                 (s/optional-key :sahkoposti) s/Str
                 :organisaatioid s/Num
                 :jarjestelma s/Bool})

(s/defschema User+Privileges (assoc User :privileges [s/Keyword]))