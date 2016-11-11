(ns hiiop.html
  (:require [rum.core :as rum]))

(rum/defc page [head body]
  [:html head body])

(rum/defc display-event [event]
  [:li event])

(rum/defc list-events [events]
  [:ul
   (map display-event events)])

(rum/defc body-content [content scripts]
  [:body content scripts])

(rum/defc head-content [{:keys [title asset-path]}]
  [:head
   [:title title]
   [:meta {:charset "UTF-8"}]
   [:link {:href (str asset-path "/css/screen.css") :rel "stylesheet" :type "text/css"}]])

(defn app-structure [{:keys [asset-path content csrf-token servlet-context]}]
  (page
   (head-content {:title "OP-100" :asset-path asset-path})
   (body-content
    [:div {:id "app"} content]
    [:script {:src (str asset-path "/js/app.js") :type "text/javascript"}])))
