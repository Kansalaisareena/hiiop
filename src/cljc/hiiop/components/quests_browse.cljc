(ns hiiop.components.quests-browse
  (:require [clojure.string :as str]
            [rum.core :as rum]
            [taoensso.timbre :as log]
            [schema.core :as s]
            [schema.coerce :as sc]
            #?(:cljs [cljs.core.async :refer [<!]])
            #?(:cljs [hiiop.client-api :as api])
            [hiiop.time :as time]
            [hiiop.components.quest-card :refer [quest-card-browse]]
            [hiiop.components.core :as c]
            [hiiop.html :as html]
            [hiiop.schema :as hs]))

(defn- sort-quests-by-latest-date [quests]
  (sort-by #(time/from-string (:start-time %))
           time/before?
           quests))

(defn- split-quests-by-months [quests]
  (let [result (group-by #(keyword
                            (time/to-string
                              (time/from-string (:start-time %)) time/year-month-format))
                         quests)]
    result))

(defn- filter-by-categories
  [{:keys [quests
           quest-filter]}]
  (if (empty? (:categories quest-filter))
    {:quests quests :quest-filter quest-filter}
    (let [categories (set (:categories quest-filter))]
      {:quests (filter #(not
                 (empty?
                   (clojure.set/intersection
                     (set (map keyword (:categories %)))
                     categories)))
                       quests)
       :quest-filter quest-filter})))

(defn- filter-by-location
  [{:keys [quests quest-filter]}]
  (let [location-filter (:location quest-filter)]
    (if (empty? location-filter)
      {:quests quests :quest-filter quest-filter}
      {:quests (filter
                 (fn [quest]
                   (let [{:keys [town
                                 country]} (:location quest)]
                     (and
                       (= town (:town location-filter))
                       (= country (:country location-filter)))))
                 quests)
       :quest-filter quest-filter})))

(defn- filter-by-end-time
  [{:keys [quests quest-filter]}]
  (let [end-time-filter (:end-time quest-filter)]
    (if (= "" end-time-filter)
      {:quests quests :quest-filter quest-filter}
      {:quests (filter
                 #(time/after?
                    (time/from-string (:end-time %))
                    (time/from-string end-time-filter))
                 quests)
       :quest-filter quest-filter})))

(defn filter-past-events [quests]
  (filter
    #(time/after?
       (time/from-string (:end-time %))
       (time/now))
    quests))

(defn apply-filters
  [{:keys [quests quest-filter]}]
  (:quests
   ((comp filter-by-end-time
          filter-by-location
          filter-by-categories)
    {:quests (if (= "" (:end-time quest-filter))
               (filter-past-events quests)
               quests)
     :quest-filter quest-filter})))

(rum/defc quest-category-icon < rum/reactive
  [{:keys [category categories]}]
  [:span
   {:class (str "opux-icon-circled opux-icon--filter opux-icon--filter--" (name category))
    :on-click (fn [_]
                (reset! categories
                        (into []
                              (filter #(not (= category %))
                                      @categories))))}])

(rum/defcs quest-categories-filter < rum/reactive
  (rum/local false ::is-active)
  [state {:keys [cursors-and-schema context categories]}]
  (let [is-active (::is-active state)
        tr (:tr context)]
    [:div {:class (str "opux-card-filter__field opux-card-filter__field--category"
                       (if (rum/react is-active) " is-active"))}
     [:div {:class "opux-card-filter__label"}
      (tr [:pages.quest.list.filter.category])]

     [:span {:class "opux-icon opux-icon-plus opux-category-filter-switch"
             :on-click #(swap! is-active not)}]

     (if (not-empty (rum/react categories))
       (map #(quest-category-icon {:category %
                                   :categories categories})
            (rum/react categories)))

     (html/form-section
       ""
       (html/multi-selector-for-schema
         {:schema (get-in cursors-and-schema [:categories :schema])
          :value (get-in cursors-and-schema [:categories :value])
          :error (get-in cursors-and-schema [:categories :error])
          :choice-name-fn hs/category-choice
          :context context}))]))

(defn- quest-location-filter
  [{:keys [cursors-and-schema context]}]
  (let [tr (:tr context)]
    [:div {:class "opux-card-filter__field opux-card-filter__field--location"}
     [:div {:class "opux-card-filter__label"}
      (tr [:pages.quest.list.filter.where])]
     (html/location-selector
       {:class "opux-input opux-input--location-selector"
        :location (get-in cursors-and-schema [:location :value])
        :error (get-in cursors-and-schema [:location :error])
        :schema (get-in cursors-and-schema [:location :schema])
        :placeholder (tr [:pages.quest.edit.location.placeholder])
        :context context
        :search-type "geocode"})]))

(defn- quest-end-time-filter
  [{:keys [context cursors-and-schema]}]
  (let [tr (:tr context)
        end-time (get-in cursors-and-schema [:end-time :value])
        end-time-object (if (not (= @end-time ""))
                          (time/from-string @end-time time/transit-format)
                          nil)
        end-time-atom (atom end-time-object)]

    (add-watch
      end-time-atom
      :filter-end-time
      (fn [key _ _ new-time]
        (let [new-end-time (time/time-to @end-time-atom 0 0)]
          (reset! end-time
                  (time/to-string
                    new-end-time
                    time/transit-format)))))

    [:div {:class "opux-card-filter__field opux-card-filter__field--datetime"}
     [:div {:class "opux-card-filter__label"}
      (tr [:pages.quest.list.filter.when])]
     (html/datepicker {:date end-time-atom
                       :position "bottom left"
                       :use-value true
                       :context context
                       :format time/date-print-format})
     [:div {:class "opux-card-filter__label"}
      (if (not-empty @end-time)
        [:a {:href "#"
             :on-click (fn [e]
                         (.preventDefault e)
                         (reset! end-time ""))}
         (tr [:pages.quest.list.filter.clear])])]]))

(defn- quest-filters
  [{:keys [tr cursors-and-schema context quest-filter]}]
  [:div {:class "opux-content opux-card-filter"}
   (quest-categories-filter {:context context
                             :cursors-and-schema cursors-and-schema
                             :categories (get-in cursors-and-schema [:categories :value])})

   (quest-location-filter {:context context
                           :cursors-and-schema cursors-and-schema})
   (quest-end-time-filter {:context context
                           :cursors-and-schema cursors-and-schema})])

(defn- quest-card-list [{:keys [quests context]}]
  (let [tr (:tr context)]
    [:ul {:class "opux-card-list"}
     (map #(quest-card-browse {:context context
                               :quest %})
          (sort-quests-by-latest-date quests))]))

(defn- monthly-quest-list
  [{:keys [quests context]}]
  (let [tr (:tr context)
        start-time (time/from-string (:start-time (first quests)))
        month-name (nth (tr [:pikaday.months])
                        (time/month start-time))]
    [:div
     [:h2 {:class "opux-centered"} month-name]
     (quest-card-list {:quests quests
                       :context context})]))

(rum/defc list-quests < rum/reactive
  [{:keys [context quests quest-filter schema errors]}]
  (let [tr (:tr context)
        filtered-quests (atom (apply-filters
                                {:quests quests
                                 :quest-filter @quest-filter}))
        quests-by-months (split-quests-by-months (rum/react filtered-quests))
        cursors-and-schema
        (c/value-and-error-cursors-and-schema {:for quest-filter
                                               :schema schema
                                               :errors errors})]

    (add-watch
      quest-filter
      :quest-filter
      (fn [key _ _ new-filter]
        (reset! filtered-quests
                (sort-quests-by-latest-date
                  (apply-filters {:quests quests
                                  :quest-filter new-filter})))

        ;; Update window location hash
        #?(:cljs
           (if (not-empty (:categories new-filter))
             (aset js/location "hash"
                   (-> new-filter
                       (:categories)
                       (#(map name %1))
                       (#(clojure.string/join "&categories[]=" %1))
                       (#(str "#?categories[]=" %1))))
             (aset js/location "hash" "")))))

    [:div {:class "opux-section"}
     [:h1 {:class "opux-centered"}
      (tr [:pages.quest.list.title])]

     (quest-filters {:cursors-and-schema cursors-and-schema
                     :quest-filter quest-filter
                     :tr tr
                     :context context})

     [:div {:class "opux-card-list-container"}
      [:div {:class "opux-content"}
       (if (empty? (rum/react filtered-quests))
         [:h1 {:class "opux-content opux-centered"}
          (tr [:pages.quest.list.not-found])]

         (if (empty? (:end-time @quest-filter))
           ;; Monthly view without end-date filter
           (map #(monthly-quest-list
                   {:quests (quests-by-months %)
                    :context context})
                (sort
                  (keys quests-by-months)))

           ;; Continuous list with end-date filter
           (quest-card-list {:quests (rum/react filtered-quests)
                             :context context})))]]]))
