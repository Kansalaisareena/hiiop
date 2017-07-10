(ns hiiop.components.index
  (:require [bidi.bidi :refer [path-for]]
            [hiiop.components.core :as c]
            [hiiop.html :as html]
            [hiiop.routes.page-hierarchy :refer [hierarchy]]
            [hiiop.schema :as hs]
            [rum.core :as rum]
            [taoensso.timbre :as log]
            [hiiop.components.social-buttons :refer [social-buttons]]))

(defn- banner [{:keys [tr]}]
  [:div {:class "opux-banner"}
   [:div {:class "opux-banner__content opux-centered"}
    [:div {:class "opux-banner__title"} (tr [:pages.index.banner.header])]
    [:div {:class "opux-banner__body-text"} (tr [:pages.index.banner.content])]]])

(defn- share-buttons [{:keys [title url tr]}]
  [:div {:class "opux-frontpage-share"}
   [:div {:class "opux-centered opux-social-sharing-buttons"}
    (social-buttons {:title title :url url :tr tr})]
   [:div {:class "opux-line opux-content"}]])

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
        blog-base-url (:hiiop-blog-base-url context)
        locale-string (name (:current-locale context))
        static-page-url (str blog-base-url "/"
                             locale-string "/blog/index.html")
        items [{:class       "opux-index-links__item--browse-quests"
                :content     (tr [:pages.index.index-links.browse-quests-text])
                :button-text (tr [:pages.index.index-links.browse-quests])
                :button-link (path-for hierarchy :browse-quests)}

               {:class       "opux-index-links__item--create-quest"
                :content     (tr [:pages.index.index-links.create-quest-text])
                :button-text (tr [:pages.index.index-links.create-quest])
                :button-link (path-for hierarchy :create-quest)}

               {:class       "opux-index-links__item--read-stories"
                :content     (tr [:pages.index.index-links.read-stories-text])
                :button-text (tr [:pages.index.index-links.read-stories])
                :button-link static-page-url}]]
    
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

(defn- counter [{:keys [tr counter-days]}]
  (let [workdays-in-month 18.5
        months-in-year 12
        goal-years 100
        percentage (float (* 100 (/ counter-days
                                    (* workdays-in-month months-in-year goal-years))))
        percentage-string (clojure.string/replace-first (str percentage) #"," ".")
        goal-achieved (>= percentage 100)
        years (int (/ counter-days
                      (* workdays-in-month months-in-year)))
        months (int (/ (- counter-days (* workdays-in-month months-in-year years))
                       workdays-in-month))
        days (int (- counter-days
                    (* workdays-in-month months)
                    (* workdays-in-month months-in-year years)))
        label (str
                (if (> years 0) (tr [:pages.index.counter.years] [years])) " "
                (if (> months 0) (tr [:pages.index.counter.months] [months])) " "
                (if (> days 0) (tr [:pages.index.counter.days] [days])))]
   [:div {:class "opux-section"}
    [:div {:class "opux-content opux-centered"}
     [:h1 (tr [:pages.index.counter.title])]
     [:div {:class "opux-banner__body-text"} (tr [:pages.index.counter.content])]
     [:div {:class "opux-counter-wrapper" }
       [:div {:class "opux-counter"}
        [:div {:class "opux-counter__goal-pin"}
         [:div {:class "opux-counter__goal-pin-label"} (tr [:pages.index.counter.hundred-years])]]
        [:div {:class "opux-counter__progress-wrapper"}
         [:div {:class (str "opux-counter__progress" (if goal-achieved " is-full"))
                :style {:width (if goal-achieved "100%" (str percentage-string "%"))}}
          [:div {:class (str "opux-counter__progress__current-pin" (if goal-achieved " is-hidden"))}
           [:div {:class "opux-counter__progress__current-pin-label"} label]]]]]]]]))

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
      [:h1 (tr [:pages.index.category-selector.title])]]

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
  [{:keys [context category-filter schema counter-days url]}]
  (let [tr (:tr context)
        url (or (get-in context [:config :site-base-url])
                (:site-base-url context))]
    [:div {:class "opux-section"}
     (banner {:tr tr})
     (counter {:tr tr :counter-days counter-days})
     (share-buttons {:tr tr :title (tr [:pages.index.title])
                     :url url})
     (category-selector {:context         context
                         :category-filter category-filter
                         :schema          schema})
     (index-links {:context context})
     (social-feed {:context context})]))
