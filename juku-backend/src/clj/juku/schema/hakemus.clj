(ns juku.schema.hakemus
  (:import (org.joda.time DateTime)
           (org.joda.time LocalDate))
  (:require [schema.core :as s]
            [clj-time.core :as time]))

(s/defschema Hakuaika {:alkupvm LocalDate
                       :loppupvm LocalDate})

(s/defschema Hakemus
  "Tämä skeema määrittää hakemuksen perustiedot."

  { :id     s/Num
    :diaarinumero     (s/maybe s/Str)
    :vuosi  s/Int
    :hakemustyyppitunnus s/Str
    :hakemustilatunnus s/Str
    :organisaatioid s/Num
    :hakuaika Hakuaika})

(s/defschema HakemusSuunnitelma
  (assoc Hakemus :haettu-avustus s/Num
                 :myonnettava-avustus s/Num))

(s/defschema Hakemus+Kasittely
  "Tämä skeema laajentaa hakemuksen perustietoja käsittelytiedolla:
  - käsittelijän nimi
  - hakemuksen muokkausaika

  Muokkausajan tarkka merkitys riippuu palvelusta. Muokkausaika voi tarkoittaa:
  1) hakemuksen sisältötietojen muokkausaikaa
  2) hakemuksen sisältötietojen ja perustietojen viimeisintä muokkausaikaa."

  (assoc Hakemus :kasittelijanimi (s/maybe s/Str)
                 :muokkausaika (s/maybe DateTime)))

(s/defschema Taydennyspyynto {:numero s/Int
                              :maarapvm LocalDate
                              :selite (s/maybe s/Str)})

(s/defschema Hakemusviite {:id     s/Num
                           :hakemustyyppitunnus s/Str})

(s/defschema Hakemus+
  (assoc Hakemus+Kasittely
    :selite (s/maybe s/Str)
    (s/optional-key :taydennyspyynto) Taydennyspyynto
    :other-hakemukset [Hakemusviite]
    :contentvisible s/Bool
    :kasittelija (s/maybe s/Str)
    :luontitunnus s/Str
    :muokkaaja (s/maybe s/Str)        ; hakemuksen sisältöön viimeisimmän muokkauksen tehnyt hakija (avustuskohteet + liitteet) (fullname)
    :lahettaja (s/maybe s/Str)        ; hakemuksen viimeisimmän lähetyksen tehnyt hakija (fullname)
    :lahetysaika (s/maybe DateTime))) ; hakemuksen viimeisin lähetysaika


(s/defschema NewHakemus (dissoc Hakemus :id :hakemustilatunnus :muokkausaika :hakuaika))

(s/defschema Hakemuskausi {:vuosi      s/Int
                           :hakemukset [Hakemus]})

(s/defschema Avustuskohde {:hakemusid     s/Num
                           :avustuskohdeluokkatunnus s/Str
                           :avustuskohdelajitunnus s/Str
                           :haettavaavustus s/Num,
                           (s/optional-key :alv) s/Num
                           :omarahoitus s/Num})

(s/defschema Avustuskohde+alv Avustuskohde #_(assoc Avustuskohde :alv s/Num))

(s/defschema Luokka {:tunnus   s/Str
                     :nimi     s/Str
                     :jarjetys s/Num})

(s/defschema Avustuskohdeluokka (assoc Luokka :avustuskohdelajit [(assoc Luokka :avustuskohdeluokkatunnus s/Str)]))

(s/defschema NewTaydennyspyynto {:hakemusid s/Num
                                (s/optional-key :selite) (s/maybe s/Str)})