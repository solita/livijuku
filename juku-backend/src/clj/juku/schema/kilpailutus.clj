(ns juku.schema.kilpailutus
  (:import (org.joda.time LocalDate))
  (:require [schema.core :as s]))

(s/defschema EditKilpailutus
  {
    :organisaatioid s/Num
    :kohdenimi s/Str
    :kohdearvo (s/maybe s/Num)
    :kalusto   (s/maybe s/Num)
    :selite    (s/maybe s/Str)

    :julkaisupvm               (s/maybe LocalDate)
    :tarjouspaattymispvm       (s/maybe LocalDate)
    :hankintapaatospvm         (s/maybe LocalDate)
    :liikennointialoituspvm    (s/maybe LocalDate)
    :liikennointipaattymispvm  (s/maybe LocalDate)
    :hankittuoptiopaattymispvm (s/maybe LocalDate)
    :optiopaattymispvm         (s/maybe LocalDate)

    :optioselite (s/maybe s/Str)

    :liikennoitsijanimi (s/maybe s/Str)
    :tarjousmaara  (s/maybe s/Num)
    :tarjoushinta1 (s/maybe s/Num)
    :tarjoushinta2 (s/maybe s/Num)
   })

(s/defschema Kilpailutus (assoc EditKilpailutus :id s/Num))