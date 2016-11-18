(ns hiiop.html
  (:require [rum.core :as rum]
            [taoensso.timbre :as log]))

(rum/defc page [head body]
  [:html head body])

(rum/defc body-content [content scripts]
  [:body content scripts])

(rum/defc head-content [{:keys [title asset-path]}]
  [:head
   [:title title]
   [:meta {:charset "UTF-8"}]
   [:link {:href (str asset-path "/css/screen.css") :rel "stylesheet" :type "text/css"}]])

(defn app-structure [{:keys [asset-path context title content csrf-token servlet-context]}]
  (let [tr (:tr context)]
    (page
     (head-content {:title (tr [:title] [title]) :asset-path asset-path})
     (body-content
      [:div {:id "app" :dangerouslySetInnerHTML {:__html content}}]
      [:script {:src (str asset-path "/js/app.js") :type "text/javascript"}]))))
