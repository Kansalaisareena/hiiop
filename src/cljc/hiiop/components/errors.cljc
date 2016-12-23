(ns hiiop.components.errors
  (:require [rum.core :as rum]))

(rum/defc error [{:keys [title content]}]
  [:div {:class "opux-content"}
   [:h1 title]
   [:p content]])
