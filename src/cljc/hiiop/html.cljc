(ns hiiop.html
  (:require [rum.core :as rum]
            [taoensso.timbre :as log]
            [bidi.bidi :refer [path-for]]))

(rum/defc page [head body]
  [:html head body])

(rum/defc top-navigation [{:keys [hierarchy tr]}]
  [:nav
   [:ul
    {:class "navigation"}
    [:li
     {:class "event find-event"}
     [:a
      {:href (path-for hierarchy :events-index)}
      (tr [:actions.events.browse])]]
    [:li
     {:class "event create-event"}
     [:a
      {:href (path-for hierarchy :create-event)}
      (tr [:actions.events.create])]]]
   [:ul
    {:class "languages"}
    [:li "fi"]
    [:li "sv"]]])

(rum/defc header [{:keys [hierarchy tr asset-path] :as context}]
  [:header
   [:h1
    {:class "name"}
    [:a
     {:href (path-for hierarchy :index)}
     (tr [:name])]]
   (top-navigation context)])

(rum/defc body-content [header content scripts]
  [:body header content scripts])

(rum/defc head-content [{:keys [title asset-path]}]
  [:head
   [:title title]
   [:meta {:charset "UTF-8"}]
   [:link {:href (str asset-path "/css/screen.css") :rel "stylesheet" :type "text/css"}]])

(defn app-structure [{:keys [context title content csrf-token servlet-context]}]
  (let [tr (:tr context)
        asset-path (:asset-path context)]
    (page
     (head-content {:title (tr [:title] [title]) :asset-path asset-path})
     (body-content
      (header context)
      [:div {:id "app" :dangerouslySetInnerHTML {:__html content}}]
      [:script {:src (str asset-path "/js/app.js") :type "text/javascript"}]))))
