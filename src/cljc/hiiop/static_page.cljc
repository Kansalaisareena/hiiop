(ns hiiop.static-page
  (:require [rum.core :as rum]))

(defn static-page [{:keys [headline body-text]}]
  [:html
   [:head
    [:meta {:charset "UTF-8"}]
    [:title (str "Hiiop: " headline)]]
   [:h1 headline]
   [:div {:dangerouslySetInnerHTML {:__html body-text}}]])
