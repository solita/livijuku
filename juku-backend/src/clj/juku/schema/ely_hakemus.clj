(ns juku.schema.ely-hakemus
  (:require [schema.core :as s]))

(s/defschema Maararahatarve
  "Tämä skeema määrittää ely-hakemuksen määrärahatarpeen tiedot." {

   :maararahatarvetyyppitunnus s/Str,
   :sidotut                    (s/maybe s/Num)
   :uudet                      (s/maybe s/Num)
   :tulot                      (s/maybe s/Num)
   :kuvaus                     (s/maybe s/Str)
})

(s/defschema Kehityshanke
  "Tämä skeema määrittää ely-hakemuksen kehityshankkeen tiedot." {

   :numero s/Num
   :nimi   s/Str
   :arvo   s/Num
   :kuvaus (s/maybe s/Str)
})

(s/defschema ELY-hakemus
  "Tämä skeema määrittää ely-hakemuksen perustiedot."
  {
   :siirtymaaikasopimukset s/Num   ; ELY hakemuksen siirtymäajan sopimukset
   :joukkoliikennetukikunnat s/Num ; ELY hakemuksen joukkoliikennetuki kunnille
  })