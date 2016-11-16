(ns hiiop.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [rum.core :as rum]
            [hiiop.html :as html]
            [hiiop.client-config :refer [env]]
            [cljs.core.async :refer [<!]]))

(enable-console-print!)

(defn get-config-and-call [this]
  (go
    (let [conf (<! @env)]
      (this conf))))

(defn render! [conf]
  (println "WAT" conf)
  (rum/mount (html/list-events ["a" "b" "c" "d"])
             (. js/document (getElementById "app"))))

(defn mount-components []
  (get-config-and-call render!))

(defn init! []
  (println "INIT")
  (get-config-and-call render!))
