(ns juku.schema.hakemus
  (:require [schema.core :as s]
            [schema.coerce :as scoerce]
            [juku.schema.yleiset :as y]))

(s/defschema Hakemus {:id        s/Num
                      :vuosi     s/Num})

(s/defschema Hakemukset [Hakemus])

