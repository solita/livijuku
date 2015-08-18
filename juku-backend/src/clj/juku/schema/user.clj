(ns juku.schema.user
  (:require [schema.core :as s]))

(s/defschema User {:tunnus  s/Str
                  (s/optional-key :etunimi) s/Str
                  (s/optional-key :sukunimi) s/Str
                  (s/optional-key :nimi) s/Str
                   :sahkoposti (s/maybe s/Str)
                   :sahkopostiviestit s/Bool
                   :organisaatioid s/Num
                   :jarjestelma s/Bool})

(s/defschema EditUser {:sahkopostiviestit s/Bool})

(s/defschema User+Privileges (assoc User :privileges [s/Keyword] :roolit [s/Str]))