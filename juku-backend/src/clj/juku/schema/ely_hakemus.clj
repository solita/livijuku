(ns juku.schema.ely-hakemus
  (:require [schema.core :as s]))

(s/defschema Maararahatarve
  "Tämä skeema määrittää ely-hakemuksen määrärahatarpeen tiedot." {

   :maararahatarvetyyppitunnus s/Str,
   :sidotut                    s/Num
   :uudet                      s/Num
   (s/optional-key :tulot)     s/Num
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
   :kaupunkilipputuki s/Num
   :seutulipputuki s/Num
   :ostot s/Num
   :kehittaminen s/Num
  })