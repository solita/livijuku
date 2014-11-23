(ns juku.schema.hakemus
  (:require [schema.core :as s]
            [schema.coerce :as scoerce]
            [juku.schema.yleiset :as y]
            [clj-time.core :as time]))

(s/defschema Hakemus {:id        s/Num
                      :vuosi     s/Num
                      :hakuaika {
                          :alkupvm org.joda.time.LocalDate
                          :loppupvm org.joda.time.LocalDate
                      }})

(s/defschema Hakemukset [Hakemus])

