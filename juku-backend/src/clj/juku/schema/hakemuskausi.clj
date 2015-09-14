(ns juku.schema.hakemuskausi
  (:require [schema.core :as s]
            [juku.schema.hakemus :as h]))

(s/defschema Hakemuskausi+ {:vuosi      s/Int
                            :tilatunnus s/Str
                            :hakuohje_contenttype (s/maybe s/Str)})

(s/defschema Hakemuskausi+Hakemukset (assoc Hakemuskausi+ :hakemukset [h/Hakemus+Kasittely]))

(s/defschema Maararaha {:maararaha s/Num
                        :ylijaama s/Num})

(s/defschema HakemustilaCount {:hakemustilatunnus s/Str
                               :count s/Num})

(s/defschema HakemusSummary {:hakemustyyppitunnus s/Str
                             :hakemustilat [HakemustilaCount]
                             :hakuaika h/Hakuaika})

(s/defschema Hakemuskausi+Summary (assoc Hakemuskausi+ :hakemukset [HakemusSummary]))

(s/defschema Hakuaika+ (assoc h/Hakuaika :hakemustyyppitunnus s/Str))
