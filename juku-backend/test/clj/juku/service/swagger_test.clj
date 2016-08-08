(ns juku.service.swagger-test
  (:require [midje.sweet :refer :all]
            [compojure.api.validator :as v]
            [juku.handler :as handler]))

(fact "Swagger API specification is valid"
      (v/validate handler/juku-api))
