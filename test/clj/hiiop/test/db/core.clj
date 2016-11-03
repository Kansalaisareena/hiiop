(ns hiiop.test.db.core
  (:require [hiiop.db.core :refer [*db*] :as db]
            [luminus-migrations.core :as migrations]
            [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [hiiop.config :refer [env]]
            [mount.core :as mount]))

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
    (is (= 1 (db/create-user!
              t-conn
              {:id    "dee3817e-87d8-44ca-9410-3649f75d09c8"
               :email "sam.smith@example.com" })))
    (is (= {:id         "dee3817e-87d8-44ca-9410-3649f75d09c8"
            :email      "sam.smith@example.com"
            :pass       nil
            :moderator  false
            :last_login nil
            :is_active  false}
           (db/get-user t-conn {:id "1"})))))
