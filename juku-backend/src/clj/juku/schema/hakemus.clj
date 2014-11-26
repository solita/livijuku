(ns juku.schema.hakemus
  (:import (org.joda.time LocalDate))
  (:require [schema.core :as s]
            [schema.coerce :as scoerce]
            [juku.schema.yleiset :as y]
            [clj-time.core :as time]))

(s/defschema Hakuaika {:alkupvm LocalDate
                       :loppupvm LocalDate})

(s/defschema Hakemus {:id     s/Num
                      :vuosi  s/Int
                      :nro    s/Int
                      :hakuaika Hakuaika})

(s/defschema Hakemukset [Hakemus])

(s/defschema Hakemuskausi {:vuosi      s/Int
                           :hakemukset [Hakemus]})

(s/defschema Hakemuskaudet [Hakemuskausi])


