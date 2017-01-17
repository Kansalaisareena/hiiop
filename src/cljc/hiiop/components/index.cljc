(ns hiiop.components.index
  (:require [bidi.bidi :refer [path-for]]
            [hiiop.components.core :as c]
            [hiiop.html :as html]
            [hiiop.routes.page-hierarchy :refer [hierarchy]]
            [hiiop.schema :as hs]
            [rum.core :as rum]))

(defn- banner [{:keys [tr]}]
  [:div {:class "opux-banner"}
   [:div {:class "opux-banner__content opux-centered"}
    [:div {:class "opux-banner__title"} (tr [:pages.index.banner.header])]
    [:div {:class "opux-banner__body-text"} (tr [:pages.index.banner.content])]]])

(defn- index-link-item
  [{:keys [class content button-text button-link]}]
  (let [class (if (nil? class) "" (str " " class))]
    [:div {:class (str "opux-index-links__item opux-centered" class)}
     [:p content]
     [:a {:class "opux-button opux-button--white"
          :href  button-link}
      button-text]]))

(defn- index-links
  [{:keys [context]}]
  (let [tr    (:tr context)
        items [{:class       "opux-index-links__item--browse-quests"
                :content     (tr [:pages.index.index-links.browse-quests-text])
                :button-text (tr [:pages.index.index-links.browse-quests])
                :button-link (path-for hierarchy :browse-quests)}

               {:class       "opux-index-links__item--create-quest"
                :content     (tr [:pages.index.index-links.create-quest-text])
                :button-text (tr [:pages.index.index-links.create-quest])
                :button-link (path-for hierarchy :create-quest)}

               ;; {:class       "opux-index-links__item--read-stories"
               ;;  :content     (tr [:pages.index.index-links.read-stories-text])
               ;;  :button-text (tr [:pages.index.index-links.read-stories])
               ;;  :button-link "#"}
               ]]
    [:div {:class "opux-section opux-index-links opux-centered"}
     (map #(index-link-item %) items)]))

(defn- generate-quest-search-link [categories]
  (let [browse-quests-path (path-for hierarchy :browse-quests)]
    (if (empty? categories)
      browse-quests-path
      (str
        browse-quests-path
        "#?categories[]="
        (clojure.string/join "&categories[]=" (map name categories))))))

(rum/defcs category-selector < rum/reactive
  (rum/local (path-for hierarchy :browse-quests) ::search-link)
  [state {:keys [context category-filter schema]}]
  (let [tr                 (:tr context)
        search-link        (::search-link state)
        browse-quests-path (path-for hierarchy :browse-quests)
        cursors-and-schema (c/value-and-error-cursors-and-schema
                             {:for    category-filter
                              :schema schema
                              :errors (atom nil)})]

    (add-watch
      category-filter
      :category-filter
      (fn [key _ _ new-filter]
        (reset! search-link
                (generate-quest-search-link (:categories new-filter)))))

    [:div {:class "opux-section"}
     [:div {:class "opux-content opux-centered"}
      [:h1 (tr [:pages.index.category-selector.title])]
      [:h3 (tr [:pages.index.category-selector.subtitle])]]

     [:div {:class "opux-content opux-centered"}
      (html/form-section
       ""
       (html/multi-selector-for-schema
         {:schema         (get-in cursors-and-schema [:categories :schema])
          :value          (get-in cursors-and-schema [:categories :value])
          :error          (get-in cursors-and-schema [:categories :error])
          :choice-name-fn hs/category-choice
          :context        context}))

      [:a {:class "opux-button opux-button--highlight"
           :href (rum/react search-link)}
       (tr [:pages.index.category-selector.search])]]]))

(defn- social-feed
  [{:keys [context]}]
  (let [tr (:tr context)]
    [:div {:class "opux-section opux-social-feed"}
     [:h1 (tr [:pages.index.social-feed.title])]
     [:div {:class "opux-content"}
      [:ul {:class        "juicer-feed"
            :data-feed-id "hiiop-9a6ea220-feaa-4128-9711-a874fa79cf74"
            :data-per     12}]]]))

(rum/defc index-page
  [{:keys [context category-filter schema]}]
  (let [tr (:tr context)]
    [:div {:class "opux-section"}
     (banner {:tr tr})
     (category-selector {:context         context
                         :category-filter category-filter
                         :schema          schema})
     (index-links {:context context})
     (social-feed {:context context})]))
