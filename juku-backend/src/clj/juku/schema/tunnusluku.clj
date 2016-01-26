(ns juku.schema.tunnusluku
  (:require [schema.core :as s]))

(s/defschema Liikennetilasto
  "Tämä skeema sisältää liikenteen markkinatiedot."
  {
   :nousut (s/maybe s/Num),
   :lahdot (s/maybe s/Num),
   :linjakilometrit (s/maybe s/Num)})


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
   :lukumaara (s/maybe s/Num)})

(s/defschema Lippuhinta
  "Lipunhinta tietyssä lippuluokassa."
  {
   :vyohykemaara s/Num

   :kertalippuhinta (s/maybe s/Num)
   :kausilippuhinta (s/maybe s/Num)})

(s/defschema Lipputulo
  "Lipunmyynnin kuukausitulo tietyssä lippuluokassa."
  {
   :kuukausi s/Num

   :kertalipputulo (s/maybe s/Num)
   :arvolipputulo (s/maybe s/Num)
   :kausilipputulo (s/maybe s/Num)
   :lipputulo (s/maybe s/Num)})

(s/defschema Liikennointikorvaus
  "Kuukausittainen liikennöintikorvaus."
  {
   :kuukausi s/Num
   :korvaus (s/maybe s/Num)
   :nousukorvaus (s/maybe s/Num),
   :nousut (s/maybe s/Num)})

(s/defschema Kustannus
  "Kuukausittainen liikennöintikorvaus."
  {
   :asiakaspalvelu (s/maybe s/Num)
   :konsulttipalvelu (s/maybe s/Num)
   :lipunmyyntipalkkio (s/maybe s/Num)
   :jarjestelmat (s/maybe s/Num)
   :muutpalvelut(s/maybe s/Num)})

(s/defschema Alue
  "Alueen tiedot"
  {
   :kuntamaara (s/maybe s/Num),
   :vyohykemaara (s/maybe s/Num),
   :pysakkimaara (s/maybe s/Num),
   :maapintaala (s/maybe s/Num),
   :asukasmaara (s/maybe s/Num),
   :tyopaikkamaara (s/maybe s/Num),
   :henkilosto (s/maybe s/Num),

   :kustannus Kustannus

   :pendeloivienosuus (s/maybe s/Num),
   :henkiloautoliikennesuorite (s/maybe s/Num),
   :autoistumisaste (s/maybe s/Num),
   :asiakastyytyvaisyys (s/maybe s/Num)

   :kommentti (s/maybe s/Str)})
