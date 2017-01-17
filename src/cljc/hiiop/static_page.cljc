(ns hiiop.static-page
  (:require [rum.core :as rum]
            [hiiop.config :refer [env asset-path]]
            ))

(defn static-page [{:keys [headline body-text]}]
  [:html
   [:head
    [:meta {:charset "UTF-8"}]
    [:title (str "Hiiop: " headline)]
    [:link {:href (str (asset-path env) "/css/screen.css")
            :rel "stylesheet"
            :type "text/css"}]]
   [:h1 headline]
   [:div {:dangerouslySetInnerHTML {:__html body-text}}]
   [:script
    {:src (str (asset-path env) "/js/app.js")
     :type "text/javascript"}]])
