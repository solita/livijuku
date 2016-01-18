(ns juku.schema.tunnusluku
  (:require [schema.core :as s]))

(s/defschema Liikennetilasto
  "Tämä skeema sisältää liikenteen markkinatiedot."
  {
   :nousut (s/->Maybe s/Num),
   :lahdot (s/->Maybe s/Num),
   :linjakilometrit (s/->Maybe s/Num)})


(s/defschema Liikennekuukausi
  "Liikenteen yhden kuukausittaiset markkinatiedot kysynnästä ja tarjonnasta."
  (assoc Liikennetilasto :kuukausi s/Num))

(s/defschema Liikennepaiva
  "Liikenteen keskimääräisen talviviikon päivän markkinatiedot kysynnästä ja tarjonnasta."
  (assoc Liikennetilasto :viikonpaivaluokkatunnus s/Str))

(s/defschema Kalusto
  "Autojen lukumäärä tietyssä päästöluokassa."
  {
   :paastoluokkatunnus s/Str,
   :lukumaara (s/->Maybe s/Num)})

(s/defschema Lippuhinta
  "Lipunhinta tietyssä lippuluokassa."
  {
   :lippuluokkaluokkatunnus s/Str,
   :vyohykelukumaara s/Num
   :hinta (s/->Maybe s/Num)})

(s/defschema Lipputulo
  "Lipunmyynnin kuukausitulo tietyssä lippuluokassa."
  {
   :kuukausi s/Num
   :lippuluokkaluokkatunnus s/Str,

   :tulo (s/->Maybe s/Num)})

(s/defschema Liikennointikorvaus
  "Kuukausittainen liikennöintikorvaus."
  {
   :kuukausi s/Num
   :korvaus (s/->Maybe s/Num)
   :nousukorvaus (s/->Maybe s/Num)})
