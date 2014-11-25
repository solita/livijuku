(ns juku.schema.hakemus
  (:require [schema.core :as s]
            [schema.coerce :as scoerce]
            [juku.schema.yleiset :as y]
            [clj-time.core :as time]))

(s/defschema Hakemus {:id     s/Num
                      :vuosi  s/Int
                      :nro    s/Int
                      :hakuaika {
                          :alkupvm org.joda.time.LocalDate
                          :loppupvm org.joda.time.LocalDate
                      }})

(s/defschema Hakemukset [Hakemus])

(s/defschema Hakemuskausi {:vuosi      s/Int
                           :hakemukset Hakemukset})

(s/defschema Hakemuskaudet [Hakemuskausi])


