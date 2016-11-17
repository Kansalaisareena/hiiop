(ns ^:figwheel-no-load hiiop.app
  (:require [hiiop.core :as core]
            [devtools.core :as devtools]
            [figwheel.client :as figwheel :include-macros true]
            [mount.core :as mount]))

(enable-console-print!)

(defn reload []
  (mount/stop)
  (mount/start)
  (core/mount-components))

(figwheel/watch-and-reload
  :websocket-url "ws://localhost:3450/figwheel-ws"
  :on-jsload reload)

(devtools/install!)

(mount/start)
(core/init!)
