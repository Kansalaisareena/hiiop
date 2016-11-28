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
     {:class "stories"}
     [:a
      {:href "http://tarinat.hiiop.fi"}
      (tr [:pages.ideas.title])]]
    [:li
     {:class "quest browse-quests"}
     [:a
      {:href (path-for hierarchy :browse-quests)}
      (tr [:actions.quest.browse])]]
    [:li
     {:class "quest create-quest"}
     [:a
      {:href (path-for hierarchy :create-quest)}
      (tr [:actions.quest.create])]]]
   [:ul
    {:class "languages"}
    [:li
     [:a
      {:href "?lang=fi"}
      "fi"]
     ]
    [:li
     [:a
      {:href "?lang=sv"}
      "sv"
      ]
     ]]
   [:ul
    {:class "login"}
    [:li
     [:a
      {:href "#kirjaudu"}
      (tr [:actions.user.login])
      ]]]])

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
      [:div {:id "app" :class "app" :dangerouslySetInnerHTML {:__html content}}]
      [:div
       [:script {:src (str asset-path "/js/app.js") :type "text/javascript"}]
       [:script {:src "https://cdnjs.cloudflare.com/ajax/libs/moment-timezone/0.5.10/moment-timezone-with-data-2010-2020.min.js" :type "text/javascript"}]]))))
