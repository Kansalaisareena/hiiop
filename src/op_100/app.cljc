(ns op-100.app
  (:require [rum.core :as rum]))

(rum/defc page [head body]
  [:html head body])

(rum/defc display-project [project]
  [:li project])

(rum/defc list-projects [projects]
  [:ul
   (map display-project projects)])

(rum/defc body-content [content scripts]
  [:body content scripts])

(rum/defc head-content [title]
  [:head
   [:title title]
   [:meta {:charset "UTF-8"}]
   [:link {:href "css/style.css" :rel "stylesheet" :type "text/css"}]])

(defn app-structure [projects]
  (page
   (head-content "OP-100")
   (body-content
    [:div {:id "app"} (list-projects projects)]
    [:script {:src "js/compiled/op_100.js" :type "text/javascript"}])))
