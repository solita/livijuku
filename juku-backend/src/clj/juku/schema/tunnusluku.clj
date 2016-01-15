(ns juku.schema.tunnusluku
  (:require [schema.core :as s]))

(s/defschema Liikennetilasto
  "Tämä skeema sisältää liikenteen markkinatiedot."
  {
   :nousut (s/->Maybe s/Num),
   :lahdot (s/->Maybe s/Num),
   :linjakilometrit (s/->Maybe s/Num)})


(s/defschema Liikennekuukausi
  "Tämä skeema sisältää liikenteen yhden kuukausittaiset markkinatiedot kysynnästä ja tarjonnasta."
  (assoc Liikennetilasto :kuukausi s/Num))

(s/defschema Liikennepaiva
  "Tämä skeema sisältää liikenteen keskimääräisen talviviikon päivän markkinatiedot kysynnästä ja tarjonnasta."
  (assoc Liikennetilasto :viikonpaivaluokkatunnus s/Str))
