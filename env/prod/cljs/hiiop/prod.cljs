(ns hiiop.app
  (:require [hiiop.core :as core]
            [mount.core :as mount]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(mount/start)
(core/init!)
