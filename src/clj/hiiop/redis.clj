(ns hiiop.redis
  (:require [mount.core :refer [defstate]]
            [taoensso.carmine :as car]
            [taoensso.carmine.ring :refer [carmine-store]]
            [taoensso.timbre :as log]
            [hiiop.config :refer [env]])
  (:import [java.net URI]))

(defstate redis-connection-options
  :start (let [uri (env :redis)]
           (log/info "redis connection" uri)
           {:spec (env :redis)}))

(defstate session-store
  :start (carmine-store
          redis-connection-options))

(defmacro wcar*
  [& body]
  `(car/wcar (conj {:pool {}} redis-connection-options) ~@body))
