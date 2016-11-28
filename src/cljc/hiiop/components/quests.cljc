(ns hiiop.components.quests
  (:require [rum.core :as rum]
            [taoensso.timbre :as log]))

(rum/defc display [quest]
  [:li quest])

(rum/defc list-quests [{:keys [context quests]}]
  (let [tr (:tr context)]
    [:div
     [:h2 (tr [:pages.quest.title])]
     [:ul
      (map display quests)]]))

(rum/defc create [{:keys [context]}]
  (let [tr (:tr context)]
    [:div
     [:h1 (tr [:actions.quests.create])]]))
