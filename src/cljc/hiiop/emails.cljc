(ns hiiop.emails
  (:require [rum.core :as rum]))

(defn activate-account [{:keys [activation-url title body-text button-text]}]
  [:html
   [:h1 title]
   [:p body-text]
   [:a {:href activation-url} button-text]])


