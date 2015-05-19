(ns juku.schema.hakemus
  (:import (org.joda.time DateTime)
           (org.joda.time LocalDate))
  (:require [schema.core :as s]
            [clj-time.core :as time]))

(s/defschema Hakuaika {:alkupvm LocalDate
                       :loppupvm LocalDate})

(s/defschema Hakemus {:id     s/Num
                      :diaarinumero     (s/maybe s/Str)
                      :vuosi  s/Int
                      :hakemustyyppitunnus s/Str
                      :hakemustilatunnus s/Str
                      :organisaatioid s/Num
                      :hakuaika Hakuaika
                      :muokkausaika DateTime})

(s/defschema HakemusSuunnitelma
  (assoc Hakemus :haettu-avustus s/Num
                 :myonnettava-avustus s/Num))

(s/defschema Hakemus+
  (assoc Hakemus :selite (s/maybe s/Str)
                 :kasittelija (s/maybe s/Str)
                 :luontitunnus s/Str
                 :hakija (s/maybe s/Str)        ; deprecated - älä käytä
                 :muokkaaja (s/maybe s/Str)        ; hakemuksen sisältöön viimeisimmän muokkauksen tehnyt hakija (avustuskohteet + liitteet) (fullname)
                 :lahettaja (s/maybe s/Str)        ; hakemuksen viimeisimmän lähetyksen tehnyt hakija (fullname)
                 :lahetysaika (s/maybe DateTime))) ; hakemuksen viimeisin lähetysaika


(s/defschema NewHakemus (dissoc Hakemus :id :hakemustilatunnus :muokkausaika))

(s/defschema Hakemuskausi {:vuosi      s/Int
                           :hakemukset [Hakemus]})

(s/defschema Avustuskohde {:hakemusid     s/Num
                           :avustuskohdeluokkatunnus s/Str
                           :avustuskohdelajitunnus s/Str
                           :haettavaavustus s/Num,
                           :omarahoitus s/Num})

(s/defschema Luokka {:tunnus   s/Str
                     :nimi     s/Str
                     :jarjetys s/Num})

(s/defschema Avustuskohdeluokka (assoc Luokka :avustuskohdelajit [(assoc Luokka :avustuskohdeluokkatunnus s/Str)]))

