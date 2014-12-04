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
                      :hakuaika Hakuaika})

(s/defschema New-Hakemus (assoc (dissoc Hakemus :id) :organisaatioid s/Num))

(s/defschema Hakemukset [Hakemus])

(s/defschema Hakemuskausi {:vuosi      s/Int
                           :hakemukset [Hakemus]})

(s/defschema Hakemuskaudet [Hakemuskausi])

(s/defschema Avustuskohde {:hakemusid     s/Num
                           :avustuskohdelajitunnus s/Str
                           :haettavaavustus s/Int,
                           :omarahoitus s/Int})


