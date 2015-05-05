(ns juku.schema.liitteet
  (:import (org.joda.time DateTime)
           (java.io InputStream))
  (:require [schema.core :as s]))

(s/defschema Liite {:hakemusid        s/Num
                    :liitenumero      s/Int
                    :nimi             s/Str
                    :contenttype      s/Str})

(s/defschema New-Liite (dissoc Liite :liitenumero))

(s/defschema Liite+ (assoc Liite :sisalto InputStream))
