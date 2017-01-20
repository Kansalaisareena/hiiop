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
  [{:keys [locale title content image-url youtube-id author]}]
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
                 :class "opux-page-section opux-page-section--contentful"}
           [:div {:class "opux-section"}

            (if image-url (image-header image-url))
            (if youtube-id (youtube-header youtube-id))

            [:div {:class "opux-content"}
             [:h1 {:class "opux-contentful-title"} title]
             (when author
               [:h3 {:class "opux-contentful-author"}
                author])]

            (when (not-empty content)
              [:div {:class "opux-content"
                     :dangerouslySetInnerHTML {:__html content}}])]]
          (html/footer context)
          [:div {:class "script-tags"}])))))

(defn- story-card [{:keys [context story]}]
  (let [{:keys [title
                url
                id
                excerpt
                author
                image-url]} story]

    [:div {:class "opux-card-container"}
     [:div {:class "opux-card"}
      (when image-url
        [:div {:class "opux-card__image-container"}
         [:a {:href url}
          [:div {:class "opux-card__image"
                 :style {:background
                         (str "url(" image-url ")")}}]]])

      [:div {:class "opux-card__content"}
       [:a {:class "opux-card__title opux-sectino"
            :href url}
        title]

       [:div {:class "opux-content"} excerpt]
       (when author
         [:div {:class "opux-content"} author])]]]))

(defn- stories-card-list [{:keys [stories context]}]
  (let [tr (:tr context)]
    [:ul {:class "opux-card-list"}
     (map #(story-card {:context context
                        :story %})
          stories)]))

(defn story-index-page-structure
  [{:keys [stories locale]}]
  (let [context (create-context locale)
        tr (:tr context)
        asset-path (:asset-path context)]
    (rum/render-static-markup
      (html/page
        (html/head-content {:title "Tarina"
                           :asset-path asset-path})
        (html/body-content
          (html/header context)
          [:div {:id "app"
                 :class "opux-page-section"}
           [:div {:class "opux-section"}
            [:div {:class "opux-content"}
             [:h1 {:class "opux-centered"}
              (tr [:pages.static.stories-index-header])]
             [:p (tr [:pages.static.stories-index-subtitle])]]

            [:div {:class "opux-section"}
             [:div {:class "opux-section opux-card-list-container"}
              [:div {:class "opux-content"}
               (stories-card-list {:stories stories
                                   :context context})]]]]]
          (html/footer context)
          [:div {:class "script-tags"}
           [:script {:src (str asset-path "/js/static.js")
                     :type "text/javascript"}]])))))
