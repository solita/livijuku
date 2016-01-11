(ns juku.schema.tunnusluku
  (:require [schema.core :as s]))

(s/defschema Liikennekuukausi
  "Tämä skeema sisältää liikenteen markkinatiedot:"
  {
    :kuukausi s/Num,
    :nousut (s/->Maybe s/Num),
    :lahdot (s/->Maybe s/Num),
    :linjakilometrit (s/->Maybe s/Num)})
