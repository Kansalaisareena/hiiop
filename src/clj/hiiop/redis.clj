(ns hiiop.redis
  (:require [taoensso.carmine :as car]
            [hiiop.config :refer [env]]))

(defmacro wcar* [& body] `(car/wcar {:pool {} :spec (env :redis)} ~@body))
