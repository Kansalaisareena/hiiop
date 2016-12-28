(ns hiiop.emails
  (:require [rum.core :as rum]))

(defn simple-mail [{:keys [title body-text button-text button-url]}]
  [:html
   [:h1 title]
   [:p body-text]
   [:a {:href button-url} button-text]])



