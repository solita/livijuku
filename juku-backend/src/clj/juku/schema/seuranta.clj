(ns juku.schema.seuranta
  (:require [schema.core :as s]))

(s/defschema Liikennesuorite
  "Tämä skeema määrittää liikennesuoritteen tiedot."

{
  :hakemusid            s/Num,
  :liikennetyyppitunnus s/Str,
  :numero               s/Num,

  :suoritetyyppitunnus  s/Str,
  :nimi                 s/Str,
  :linjaautot           s/Num,
  :taksit               s/Num,
  :ajokilometrit        s/Num,
  :matkustajamaara      s/Num,
  :asiakastulo          s/Num,
  :nettohinta           s/Num,
  :bruttohinta          s/Num
})
