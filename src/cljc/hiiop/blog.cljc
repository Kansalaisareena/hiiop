(ns hiiop.blog
  (:require [rum.core :as rum]))

(defn blog-post [{:keys [headline body-text picture youtube-id]}]
  [:html
   [:head
    [:meta {:charset "UTF-8"}]
    [:title (str "Hiiop: " headline)]]
   [:img {:src picture}]
   [:h1 headline]
   [:div {:dangerouslySetInnerHTML {:__html body-text}}]
   [:div {:class :youtube-container :dangerouslySetInnerHTML {:__html (str "<iframe width=\"560\" height=\"315\" src=\"https://www.youtube.com/embed/" youtube-id "\" frameborder=\"0\" allowfullscreen></iframe>")}}]])

