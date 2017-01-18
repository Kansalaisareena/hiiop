(ns hiiop.contentful-page
  (:require [rum.core :as rum]
            [hiiop.html :as html]
            [hiiop.config :refer [env asset-path]]
            [hiiop.routes.page-hierarchy :refer [hierarchy]]
            [hiiop.translate :refer [tr-opts default-locale]]
            [taoensso.tempura :as tempura]))

(defn- create-context [locale]
  {:tr (partial tempura/tr (tr-opts) [locale])
   :config env
   :hierarchy hierarchy
   :asset-path (asset-path env)
   :identity nil
   :current-locale locale})

(defn- image-header [image-url]
  (if image-url
    [:div {:class "opux-content opux-content--image-header"
           :style {:background-image
                   (str "url(" image-url ")")}}]))

(defn- youtube-header [youtube-id]
  [:div {:class "opux-content opux-content--image-header"}
   [:iframe
    {:width "320"
     :height "auto"
     :src
     (str "https://www.youtube.com/embed/"
          youtube-id
          "?modestbranding=1&amp;fs=0&amp;rel=0&amp;hd=1&amp;disablekb=0&amp;showinfo=0&amp;iv_load_policy=0&amp;autoplay=0&amp;loop=0&amp;controls=0")
     :frameborder 0}]])

(defn contentful-page-structure
  [{:keys [locale title content image-url youtube-id]}]
  (let [context (create-context locale)
        tr (:tr context)
        asset-path (:asset-path context)]
    (rum/render-static-markup
      (html/page
        (html/head-content {:title title
                            :asset-path asset-path})
        (html/body-content
          (html/header context)
          [:div {:id "app"
                 :class "opux-page-section"}
           [:div {:class "opux-section"}
            (if image-url (image-header image-url))
            (if youtube-id (youtube-header youtube-id))
            [:h1 {:class "opux-centered"} title]
            [:div {:class "opux-content"
                   :dangerouslySetInnerHTML {:__html content}}]]]
          (html/footer context)
          [:div {:class "script-tags"}]
          ;;  (html/script-tag default-script)]
          )))))
