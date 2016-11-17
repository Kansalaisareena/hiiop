(ns hiiop.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [rum.core :as rum]
            [hiiop.html :as html]
            [hiiop.client-config :refer [env]]
            [cljs.core.async :refer [<!]]
            [hiiop.translate :refer [tr-with tr-opts]]))

(enable-console-print!)

(defn get-config-and-call [this]
  (go
    (let [conf (<! @env)]
      (this conf))))

(defn render! [{:keys [accept-langs langs] :as conf}]
  (let [tr (tr-with (tr-opts langs) accept-langs)]
    (rum/mount
     (html/list-events {:events ["a" "b" "c" "d"]
                        :tr tr})
     (. js/document (getElementById "app")))))

(defn mount-components []
  (get-config-and-call render!))

(defn init! []
  (println "INIT")
  (get-config-and-call render!))
