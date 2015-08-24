(ns juku.schema.user
  (:require [schema.core :as s])
  (:import (org.joda.time DateTime)))

(s/defschema User {:tunnus  s/Str
                  (s/optional-key :etunimi) s/Str
                  (s/optional-key :sukunimi) s/Str
                  (s/optional-key :nimi) s/Str
                   :sahkoposti (s/maybe s/Str)
                   :sahkopostiviestit s/Bool
                   :organisaatioid s/Num
                   :jarjestelma s/Bool
                   :kirjautumisaika DateTime})

(s/defschema EditUser {:sahkopostiviestit s/Bool})

(s/defschema User+Roles (assoc User :roolit [s/Str]))

(s/defschema User+Privileges (assoc User+Roles :privileges [s/Keyword]))