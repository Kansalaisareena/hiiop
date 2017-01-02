(ns hiiop.test.db.core
  (:require [hiiop.db.core :refer [*db* ->snake_case_keywords] :as db]
            [luminus-migrations.core :as migrations]
            [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [mount.core :as mount]
            [clj-time.core :as t]
            [hiiop.time :refer [add now an-hour]]
            [taoensso.timbre :as log]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [schema.coerce :as coerce]
            [hiiop.test.data :refer [test-quest]]
            [hiiop.test.util :refer [contains-many?]]
            [hiiop.config :refer [env]]))

(use-fixtures
  :once
  (fn [f]
    (mount/start
     #'hiiop.config/env
     #'hiiop.db.core/*db*)
    (migrations/migrate ["migrate"] (select-keys env [:database-url]))
    (f)))

(deftest test-users
  (jdbc/with-db-transaction [t-conn *db*]
    (jdbc/db-set-rollback-only! t-conn)
    (is (not (nil? (db/create-virtual-user!
                    t-conn
                    {:email "sam.smith@example.com"}))))
    (is (contains-many?
         (db/get-user-by-email t-conn {:email "sam.smith@example.com"})
         :id
         :name
         :email
         :phone
         :moderator
         :last-login
         :is-active))))

(deftest test-user-activation
  (jdbc/with-db-transaction [t-conn *db*]
    (jdbc/db-set-rollback-only! t-conn)
    (is (not (nil? (db/create-virtual-user!
                    t-conn
                    {:email "sam.smith@example.com"})))
        "create-virtual-user! should return user id")
    (let [token (:token (db/create-password-token!
                         t-conn
                         {:email "sam.smith@example.com"
                          :expires (add (now) an-hour)}))]
      (is (not (nil? token))
          "create-password-token! should return the token")
      (is (= true (:exists (db/check-token-validity t-conn {:token token})))
          "created token should be valid")
      (is (= 1 (db/activate-user! t-conn
                {:email "sam.smith@example.com"
                 :pass "password#"
                 :token token}))
          "activate-user! should return 1 (1 row changed)")
      (let [user (db/get-user-by-email t-conn {:email "sam.smith@example.com"})]
        (is (= true (:is-active user))
            "User should be active")))))

(deftest test-user-activation-fails-with-wrong-token
  (jdbc/with-db-transaction [t-conn *db*]
    (jdbc/db-set-rollback-only! t-conn)
    (is (not (nil? (db/create-virtual-user!
                    t-conn
                    {:email "sam@example.com"})))
        "create-virtual-user! should return user id")
    (let [token (:token (db/create-password-token!
                         t-conn
                         {:email "sam@example.com"
                          :expires (add (now) an-hour)}))]
      (is (not (nil? token))
          "create-password-token! should return the token")
      (is (= true (:exists (db/check-token-validity t-conn {:token token})))
          "created token should be valid")
      (is (= 0 (db/activate-user! t-conn
                {:email "sam@example.com"
                 :pass "password#"
                 :token (coerce/string->uuid "bc076be4-b571-11e6-9f11-338b4c8301d1")}))
          "activate-user! with random uuid should return 0 (0 rows changed)")
      (let [user (db/get-user-by-email t-conn {:email "sam@example.com"})]
        (is (= false (:is-active user))
            "User should be inactive")))))
