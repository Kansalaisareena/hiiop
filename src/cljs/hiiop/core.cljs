(ns hiiop.core
  (:require [rum.core :as rum]
            [hiiop.html :as html]
            [hiiop.client-config :refer [env]]))

(enable-console-print!)

(defn mount-components []
  (println "WAT"))

(defn init! []
  (println "INIT")
  (rum/mount (html/list-events ["a" "b" "c" "d"])
             (. js/document (getElementById "app"))))
