(ns hiiop.redis
  (:require [mount.core :refer [defstate]]
            [taoensso.carmine :as car]
            [taoensso.carmine.ring :refer [carmine-store]]
            [taoensso.timbre :as log]
            [hiiop.config :refer [env]]))

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

(defmacro get-in-cache-or [key expr]
  `(let [cached-result# (wcar* (car/get ~key))]
     (if-not cached-result#
       (let [result# ~expr]
         (wcar* (car/set ~key (car/serialize result#)))
         result#)
       cached-result#)))

(defstate clear-from-cache
  :start (fn [key]
           (wcar* (car/del key))))

(defmacro redef-with-cache [fun key]
  "Redefine an arity 0 function to be cached with the given key."
  `(let [old-fun# ~fun]
     (defn ~fun []
       (get-in-cache-or ~key (old-fun#)))))

(defmacro redef-invalidate-cache [fun key]
  "Redefine function so that it deletes with key from redis on call."
  `(let [old-fun# ~fun]
     (defn ~fun [& args#]
       (wcar* (car/del ~key))
       (apply old-fun# args#))))
