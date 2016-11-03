(ns user
  (:require [mount.core :as mount]
            [hiiop.figwheel :refer [start-fw stop-fw cljs]]
            hiiop.core))

(defn start []
  (mount/start-without #'hiiop.core/repl-server))

(defn stop []
  (mount/stop-except #'hiiop.core/repl-server))

(defn restart []
  (stop)
  (start))
