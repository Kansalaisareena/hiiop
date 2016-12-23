(ns hiiop.blog
  (:require [rum.core :as rum]))

(defn blog-post [{:keys [headline body-text picture video]}]
  [:html
   [:head
    [:meta {:charset "UTF-8"}]
    [:title (str "Hiiop: " headline)]]
   [:h1 headline]
   [:div body-text]])
