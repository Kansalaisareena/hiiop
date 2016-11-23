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

(defn contains-many? [m & ks]
  (every? #(contains? m %) ks))

(deftest test-users
  (jdbc/with-db-transaction [t-conn *db*]
    (jdbc/db-set-rollback-only! t-conn)
    (is (= 1 (db/create-virtual-user!
              t-conn
              {:email "sam.smith@example.com"})))
    (is (contains-many?
         (db/get-user-by-email t-conn {:email "sam.smith@example.com"})
         :id
         :name
         :email
         :moderator
         :last_login
         :is_active))))
