(ns hiiop.emails
  (:require [rum.core :as rum]))

(defn simple-mail [{:keys [title body-text button-text button-url message]}]
  [:html
   [:h1 title]
   [:div body-text]
   (when message [:div {:class :message} message])
   [:a {:href button-url} button-text]])



