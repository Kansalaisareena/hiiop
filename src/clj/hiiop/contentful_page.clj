(ns hiiop.contentful-page
  (:require [rum.core :as rum]
            [hiiop.html :as html]
            [hiiop.config :refer [env asset-path]]
            [hiiop.routes.page-hierarchy :refer [hierarchy]]
            [hiiop.time :as time]
            [hiiop.translate :refer [tr-opts default-locale]]
            [taoensso.tempura :as tempura]
            [hiiop.components.social-buttons :refer [social-buttons]]
            [cemerick.url :refer [url-encode]]
            [taoensso.timbre :as log]
            [hiiop.url :refer [image-url-to-small-url]]))

(defn- create-context [locale]
  {:tr (partial tempura/tr (tr-opts) [locale])
   :config env
   :hierarchy hierarchy
   :asset-path (asset-path env)
   :identity nil
   :is-static-page true
   :current-locale locale})

(defn- image-header [image-url]
  (if image-url
    [:div {:class "opux-section"}
     [:div {:class "opux-banner opux-content--image-header opux-banner--contain"
           :style {:background-image
                   (str "url(" image-url ")")}}]]))

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
  [{:keys [locale title content image-url youtube-id author excerpt url show-social-metas created-at]}]
  (let [context (create-context locale)
        tr (:tr context)
        asset-path (:asset-path context)
        metas (if-not show-social-metas
                nil
                [{:property "og:title"
                  :content title}
                 {:property "og:content"
                  :content excerpt}
                 {:property "og:type"
                  :content "article"}
                 {:property "og:url"
                  :content url}
                 {:property "og:app_id"
                  :content (get-in env [:social :facebook-app-id])}
                 {:property "og:image"
                  :content (if image-url
                             (str (:hiiop-blog-base-url env) image-url)
                             (str (:asset-base-url env) "/img/banner.jpg"))}
                 {:name "twitter:creator"
                  :content (get-in env [:social :twitter-account])}
                 {:name "twitter:card"
                  :content excerpt}])]
    (rum/render-static-markup
      (html/page
        (html/head-content {:title title
                            :asset-path asset-path
                            :metas metas
                            :locale (:current-locale context)})
        (html/body-content
          context
          (html/header context)
          [:div {:id "app"
                 :class "opux-page-section opux-page-section--contentful"}
           [:div {:class "opux-section"}

            (if image-url (image-header image-url))
            (if youtube-id (youtube-header youtube-id))

            [:div {:class "opux-content"}
             [:h3 {:class "opux-contentful-date"}
              (time/to-string created-at
                              time/date-print-format)]
             [:h1 {:class "opux-contentful-title"} title]
             (when author
               [:h3 {:class "opux-contentful-author"}
                author])]

            (when (not-empty content)
              [:div {:class "opux-content"
                     :dangerouslySetInnerHTML {:__html content}}])

            (when show-social-metas
              [:div {:class "opux-content"}
               (social-buttons {:title title
                                :url url
                                :tr tr})])]]
          (html/footer context)
          [:div {:class "script-tags"}
           [:script {:src (str asset-path "/js/static.js")
                     :type "text/javascript"}]])))))

(defn- youtube-image-url [youtube-id]
  (str
   "https://img.youtube.com/vi/"
   youtube-id
   "/mqdefault.jpg"))

(defn- story-card [{:keys [context story]}]
  (let [{:keys [title
                url
                id
                excerpt
                author
                image-url
                youtube-id
                created-at
                categories]} story
        categories-attr (url-encode
                         (clojure.string/join "," categories))
        thumbnail-url (if image-url
                        (image-url-to-small-url image-url)
                        (youtube-image-url youtube-id))]
    [:div {:class "opux-card-container"
           :categories categories-attr}
     [:div {:class "opux-card"}
      (when thumbnail-url
        [:div {:class "opux-card__image-container"}
         [:a {:href url}
          [:div {:class "opux-card__image"
                 :style {:background-image
                         (str "url(" thumbnail-url ")")}}]]])

      [:div {:class "opux-card__content"}
       [:div {:class "opux-content"}
        (time/to-string created-at
                        time/date-print-format)]
       [:a {:class "opux-card__title opux-card__title--blog"
            :href url}
        title]

       [:div {:class "opux-content"} excerpt]
       (when author
         [:div {:class "opux-content"} author])]]]))

(defn- category-filter [category]
  (when (not (nil? category))
    [:span {:class "opux-category-filter"
            :filter-data (url-encode category)}
     category]))

(defn- stories-filters [{:keys [stories context]}]
  (let [categories (-> (map :categories stories)
                       (flatten)
                       (distinct))]
    (when (not-empty categories)
      [:div {:class "opux-category-filters-container"}
       (map category-filter categories)])))

(defn- stories-card-list [{:keys [stories context]}]
  (let [tr (:tr context)]
    [:div {:class "opux-card-list"}
     (map #(story-card {:context context
                        :story %})
          stories)]))

(defn story-index-page-structure
  [{:keys [stories locale url]}]
  (let [context (create-context locale)
        tr (:tr context)
        asset-path (:asset-path context)
        metas [{:property "og:title"
                :content (tr [:pages.static.stories-index-header])}
               {:property "og:content"
                :content (tr [:pages.static.stories-index-subtitle])}
               {:property "og:type"
                :content "article"}
               {:property "og:url"
                :content url}
               {:property "og:app_id"
                :content (get-in env [:social :facebook-app-id])}
               {:property "og:image"
                :content (str (:asset-base-url env)
                              "/img/banner.jpg")}
               {:name "twitter:creator"
                :content (get-in env [:social :twitter-account])}
               {:name "twitter:card"
                :content (tr [:pages.static.stories-index-subtitle])}]]
    (str
     "<!doctype html>"
     (rum/render-static-markup
      (html/page
        (html/head-content {:title (tr [:pages.static.stories-title])
                            :asset-path asset-path
                            :metas metas
                            :locale (:current-locale context)})
        (html/body-content
          (html/header context)
          [:div {:id "app"
                 :class "opux-page-section"}
           [:div {:class "opux-section"}
            [:div {:class "opux-content"}
             [:h1 {:class "opux-centered"}
              (tr [:pages.static.stories-index-header])]
             [:div {:class "opux-centered"}
              (tr [:pages.static.stories-index-subtitle])]]

            [:div {:class "opux-section"}
             [:div {:class "opux-card-list-container"}
              [:div {:class "opux-content"}
               (stories-filters {:stories stories
                                 :context context})
               (stories-card-list {:stories stories
                                   :context context})]]]]]
          (html/footer context)
          [:div {:class "script-tags"}
           [:script {:src (str asset-path "/js/static.js")
                     :type "text/javascript"}]]))))))
