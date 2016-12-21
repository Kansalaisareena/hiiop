(ns hiiop.test.util
  (:require [buddy.hashers :as hashers]
            [ring.mock.request :refer :all]))

(defn hash-password [password]
  (hashers/derive password {:alg :bcrypt+blake2b-512}))

(defn contains-many? [m & ks]
  (every? #(contains? m %) ks))

(defn json-request [endpoint {:keys [type body-string cookies]}]
  (->
   (request type endpoint)
   (header "cookie" cookies)
   (body body-string)
   (content-type "application/json")))
