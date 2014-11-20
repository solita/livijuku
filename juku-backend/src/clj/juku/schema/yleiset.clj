(ns juku.schema.yleiset
  (:require [schema.core :as s :include-macros  true]
            [schema.coerce :as scoerce]))

(s/defschema Muokkaustiedot {:luontitunnus s/Str
                             ;:luontiaika org.joda.time.DateTime
                             :muokkaustunnus s/Str
                             ;:muokkausaika org.joda.time.DateTime
                             })
