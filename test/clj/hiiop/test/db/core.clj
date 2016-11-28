(ns hiiop.test.db.core
  (:require [hiiop.db.core :refer [*db* ->snake_case_keywords] :as db]
            [luminus-migrations.core :as migrations]
            [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [mount.core :as mount]
            [clj-time.core :as t]
            [taoensso.timbre :as log]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [hiiop.config :refer [env]]
            [hiiop.test.test :refer [contains-many?]]))

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
    (is (= 1 (db/create-virtual-user!
              t-conn
              {:email "sam.smith@example.com"})))
    (is (contains-many?
         (db/get-user-by-email t-conn {:email "sam.smith@example.com"})
         :id
         :name
         :email
         :moderator
         :last-login
         :is-active))))

(defn transform-to-quest-db-fields [{:keys [quest id is-open picture-url]}]
  (let [quest-with-added (assoc quest
                                :description nil
                                :id id
                                :is-open is-open
                                :picture-url nil)]
    (dissoc
     quest-with-added
     :picture)))

(deftest test-create-unmoderated-open-quest
  (jdbc/with-db-transaction [t-conn *db*]
    (jdbc/db-set-rollback-only! t-conn)
    (let [today-at-twelve (hiiop.time/with-default-time-zone (t/today-at 12 00))
          today-at-six (hiiop.time/with-default-time-zone (t/plus today-at-twelve (t/hours 6)))
          quest {:name "Nälkäkeräys"
                 :start-time today-at-twelve
                 :end-time today-at-six
                 :address "Raittipellontie 3"
                 :town "Kolari"
                 :categories ["foreign-aid"]
                 :unmoderated-description "LOL"
                 :max-participants 10
                 :hashtags ["a" "b" "c" "d"]
                 :picture nil
                 :owner nil}

          from-db (db/add-unmoderated-open-quest!
                   t-conn
                   (->snake_case_keywords quest))
          added-quest-id (:id from-db)
          expected-quest (transform-to-quest-db-fields
                          {:quest quest
                           :id added-quest-id
                           :is-open true
                           :picture-url nil})]
      (is (not (= (:id added-quest-id) 0)))
      (is (= expected-quest
             (db/get-quest-by-id t-conn {:id added-quest-id}))))
    ))
