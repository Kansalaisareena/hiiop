(ns op-100.db.core
  (:require
   [mount.core :refer [defstate]]
   [conman.core :as conman]))

(def pool-spec
  {:init-size 1
   :min-idle 1
   :max-idle 4
   :max-active 32
   :jdbc-url (System/getenv "DATABASE_URL")})

(defstate ^:dynamic *db*
  :start (conman/connect! pool-spec)
  :stop (conman/disconnect! *db*))

(conman/bind-connection *db* "sql/queries.sql")
