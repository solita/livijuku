(ns juku.schema.paatos
  (:import (org.joda.time DateTime))
  (:require [schema.core :as s]))

(s/defschema Paatos {:hakemusid         s/Num
                     :paatosnumero      s/Int
                     :myonnettyavustus  (s/maybe s/Num)
                     :voimaantuloaika   (s/maybe DateTime)
                     :poistoaika        (s/maybe DateTime)
                     :paattaja          (s/maybe s/Str)
                     :paattajanimi      (s/maybe s/Str)
                     :selite            (s/maybe s/Str)})

(s/defschema EditPaatos {:hakemusid         s/Num
                         :myonnettyavustus  s/Num
                         (s/optional-key :paattajanimi) (s/maybe s/Str)
                         :selite            s/Str})