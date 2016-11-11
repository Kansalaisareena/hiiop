(ns hiiop.users
  (:require [hiiop.db.core :as db] 
            [buddy.hashers :as hashers]))

(defn check-password [email password]
  "Check wether <password> is a valid password for <email>, return
  true/false."
  (let [hash (:pass (db/get-password-hash {:email email}))]
    (if-not (nil? hash)
      (hashers/check password hash)
      (hashers/check ; prevent timing attack by checking against "dummy_password"
       "wrong_password"
       "bcrypt+sha512$a8b581d6ad8e999cc72005d34bd1cbba$12$3563740bed9e7e8c2fdb2c1006229bc14dbb3970556e8921"))))

(defn get-user-id [email]
  (:id (db/get-user-id {:email email})))

(defn add-user [email password]
  (db/add-test-user!
   {:email email
    :pass (hashers/derive password)}))
