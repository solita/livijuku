(ns juku.schema.hakemuskausi
  (:require [schema.core :as s]
            [juku.schema.hakemus :as h]))


(s/defschema Hakemuskausi+ {:vuosi      s/Int
                            :tilatunnus s/Str
                            :hakuohje_contenttype s/Str
                            :hakemukset #{h/Hakemus}})

(s/defschema Maararaha {:maararaha s/Num
                        :ylijaama s/Num})


