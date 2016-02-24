(ns juku.rest-api.response
  (:require [ring.util.http-response :as http-response]
            [ring.util.codec :as codec]))

(defn content-disposition-inline [filename response]
  (if filename
    (http-response/header response "content-disposition",
                        (str "inline; filename*=UTF-8''" (codec/url-encode filename "utf-8")))
    response))
