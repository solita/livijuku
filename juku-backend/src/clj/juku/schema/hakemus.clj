(ns juku.schema.hakemus
  (:import (org.joda.time LocalDate))
  (:require [schema.core :as s]
            [clj-time.core :as time]))

(s/defschema Hakuaika {:alkupvm LocalDate
                       :loppupvm LocalDate})

(s/defschema Hakemus {:id     s/Num
                      :vuosi  s/Int
                      :hakemustyyppitunnus s/Str
                      :hakemustilatunnus s/Str
                      :organisaatioid s/Num
                      :hakuaika Hakuaika})

(s/defschema HakemusSuunnitelma
     (assoc Hakemus :haettu-avustus s/Num
                    :myonnettava-avustus s/Num))

(s/defschema Hakemus+ (assoc Hakemus :selite (s/maybe s/Str)))

(s/defschema New-Hakemus (dissoc Hakemus :id :hakemustilatunnus))

(s/defschema Hakemukset [Hakemus])

(s/defschema Hakemuskausi {:vuosi      s/Int
                           :hakemukset [Hakemus]})

(s/defschema Hakemuskaudet [Hakemuskausi])

(s/defschema Avustuskohde {:hakemusid     s/Num
                           :avustuskohdelajitunnus s/Str
                           :haettavaavustus s/Int,
                           :omarahoitus s/Int})


