(ns hiiop.components.events
  (:require [rum.core :as rum]
            [taoensso.timbre :as log]))

(rum/defc display [event]
  [:li event])

(rum/defc list-events [{:keys [context events]}]
  (let [tr (:tr context)]
    [:div
     [:h1 (tr [:pages.events.title])]
     [:ul
      (map display events)]]))

(rum/defc create [{:keys [context]}]
  (let [tr (:tr context)]
    [:div
     [:h1 (tr [:actions.events.create])]]))
