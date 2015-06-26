(ns juku.service.common
  (:require [slingshot.slingshot :as ss]
            [juku.db.yesql-patch :as sql]
            [ring.util.http-response :as r]))

; *** Yleiset kyselyt ***
(sql/defqueries "common.sql")

(defn retry [tries f & args]
  (let [res (try {:value (apply f args)}
                 (catch Exception e
                   (if (= 0 tries)
                     (throw e)
                     {:exception e})))]
    (if (:exception res)
      (recur (dec tries) f args)
      (:value res))))

(defn assert-user-is-hakemus-owner! [user hakemusids]
  (let [user-organisaatio (:organisaatioid user)
        organisaatiot (set (map :organisaatioid (select-hakemus-organisaatiot {:hakemusids hakemusids})))]
    (if (or (> (count organisaatiot) 1)
            (not= (first organisaatiot) user-organisaatio))
      (let [msg (str "Käyttäjä " (:tunnus user)
                     " ei omista hakemuksia: "
                     (reduce (fn [acc id] (str acc ", " id)) hakemusids))]
        (ss/throw+ {:http-response r/forbidden :message msg} msg)))))