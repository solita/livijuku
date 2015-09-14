(ns juku.schema.hakemuskausi
  (:require [schema.core :as s]
            [juku.schema.hakemus :as h]))

(s/defschema Hakemuskausi+ {:vuosi      s/Int
                            :tilatunnus s/Str
                            :hakuohje_contenttype (s/maybe s/Str)})

(defn hakemuskausi+hakemukset [hakemusschema] (assoc Hakemuskausi+ :hakemukset [hakemusschema]))

(s/defschema Hakemuskausi+Hakemukset+Kasittely (hakemuskausi+hakemukset h/Hakemus+Kasittely))

(s/defschema Hakemuskausi+Hakemukset (hakemuskausi+hakemukset h/Hakemus))


(s/defschema Maararaha {:maararaha s/Num
                        :ylijaama s/Num})

(s/defschema HakemustilaCount {:hakemustilatunnus s/Str
                               :count s/Num})

(s/defschema HakemusSummary {:hakemustyyppitunnus s/Str
                             :hakemustilat [HakemustilaCount]
                             :hakuaika h/Hakuaika})

(s/defschema Hakemuskausi+Summary (assoc Hakemuskausi+ :hakemukset [HakemusSummary]))

(s/defschema Hakuaika+ (assoc h/Hakuaika :hakemustyyppitunnus s/Str))
