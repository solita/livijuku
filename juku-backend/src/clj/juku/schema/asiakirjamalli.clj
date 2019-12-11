(ns juku.schema.asiakirjamalli
  (:require [schema.core :as s])
  (:import (org.joda.time DateTime)))

(s/defschema Asiakirjamalli
  {:id                     s/Num,
   :asiakirjalajitunnus    s/Str,
   :voimaantulovuosi       s/Num,
   :hakemustyyppitunnus    (s/maybe s/Str),
   :organisaatiolajitunnus (s/maybe s/Str),
   :poistoaika             (s/maybe DateTime)})

(s/defschema Asiakirjamalli+sisalto
  (assoc Asiakirjamalli :sisalto s/Str))

(s/defschema Edit-Asiakirjamalli (dissoc Asiakirjamalli :id))