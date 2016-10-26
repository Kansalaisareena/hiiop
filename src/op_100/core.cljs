(ns op-100.core
  (:require [rum.core :as rum]
             [op-100.app :as app]))

(enable-console-print!)

(println "WAT")

;; define your app data so that it doesn't get over-written on reload

(rum/mount (app/list-projects ["a" "b" "c" "d"])
           (. js/document (getElementById "app")))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
