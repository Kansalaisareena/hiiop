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

(defmacro get-in-cache-or [key expr expires]
  `(let [cached-result# (wcar* (car/get ~key))]
     (if-not cached-result#
       (let [result# ~expr]
         (if ~expires
           (wcar* (car/set ~key (car/serialize result#) :EX ~expires))
           (wcar* (car/set ~key (car/serialize result#))))
         result#)
       cached-result#)))

(defstate clear-from-cache
  :start (fn [key]
           (wcar* (car/del key))))

(defmacro redef-with-cache [fun key expires]
  "Redefine an arity 0 function to be cached with the given key."
  `(let [old-fun# ~fun]
     (defn ~fun []
       (get-in-cache-or ~key (old-fun#) ~expires))))

(defmacro redef-refresh-cache [fun refresh-expr key]
  "Redefine function so that it refreshes key in redis on call."
    `(let [old-fun# ~fun]
       (defn ~fun [& args#]
         (let [ret# (apply old-fun# args#)]
           (wcar* (car/del ~key)
                  (car/set ~key (car/serialize (~refresh-expr))))
           ret#))))

(defmacro redef-invalidate-cache [fun key]
  "Redefine function so that it deletes with key from redis on call."
  `(let [old-fun# ~fun]
     (defn ~fun [& args#]
       (wcar* (car/del ~key))
       (apply old-fun# args#))))
