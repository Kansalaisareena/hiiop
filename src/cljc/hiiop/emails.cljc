(ns hiiop.emails
  (:require [rum.core :as rum]))

(defn simple-mail [{:keys [title body-text button-text button-url
                           message button2-text button2-url]}]
  [:html
   [:h1 title]
   [:div body-text]
   (when message [:div {:class :message} message])
   [:a {:href button-url :class :button :id :button1} button-text]
   (when button2-text
     [:a {:href button-url :class :button :id :button2} button2-text])])



