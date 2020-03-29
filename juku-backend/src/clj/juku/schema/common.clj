(ns juku.schema.common
  (:require [schema.core :as s :include-macros true]
            [schema.coerce :as scoerce]
            [clojure.string :as str]))

(s/defschema Muokkaustiedot {:luontitunnus s/Str
                             :luontiaika org.joda.time.DateTime
                             :muokkaustunnus s/Str
                             :muokkausaika org.joda.time.DateTime})

(s/defschema Luokka {:tunnus   s/Str
                     :nimi     s/Str
                     :jarjestys s/Num})

(def NotBlank (s/constrained s/Str (complement str/blank?)))
