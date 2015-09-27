(ns juku.schema.seuranta
  (:require [schema.core :as s]))

(s/defschema Liikennesuorite
  "Tämä skeema määrittää liikennesuoritteen tiedot."

{
  :liikennetyyppitunnus s/Str,
  :numero               s/Num,

  :suoritetyyppitunnus  s/Str,
  :nimi                 s/Str,
  :linjaautot           s/Num,
  :taksit               s/Num,
  :ajokilometrit        s/Num,
  :matkustajamaara      s/Num,
  :lipputulo            s/Num,
  :nettohinta           s/Num
})
