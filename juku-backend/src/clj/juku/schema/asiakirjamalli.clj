(ns juku.schema.asiakirjamalli
  (:require [schema.core :as s]))

(s/defschema Asiakirjamalli
  {:id s/Num,
   :asiakirjalajitunnus s/Str,
   :voimaantulovuosi s/Num,
   :hakemustyyppitunnus s/Str,
   :organisaatiolajitunnus s/Str,
   :sisalto s/Str,
   :poistoaika DateTime})