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

(defn- split-quests-by-months [quests]
  (let [result (group-by #(time/month
                            (time/from-string
                              (:start-time %)))
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

(defn- filter-by-start-time
  [{:keys [quests quest-filter]}]
  (let [start-time-filter (:start-time quest-filter)]
    (if (= "" start-time-filter)
      {:quests quests :quest-filter quest-filter}
      {:quests (filter
                 #(time/after?
                    (time/from-string (:start-time %))
                    (time/from-string start-time-filter))
                 quests)
       :quest-filter quest-filter})))

(defn filters
  [{:keys [quests quest-filter]}]
  (:quests ((comp filter-by-start-time
                  filter-by-location
                  filter-by-categories)
            {:quests quests
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

(defn- quest-start-time-filter
  [{:keys [context cursors-and-schema]}]
  (let [tr (:tr context)
        start-time (get-in cursors-and-schema [:start-time :value])
        start-time-object (if (not (= @start-time ""))
                            (time/from-string @start-time time/transit-format)
                            nil)
        start-time-atom (atom start-time-object)]

    (add-watch
      start-time-atom
      :filter-start-time
      (fn [key _ _ new-time]
        (let [new-start-time (time/time-to @start-time-atom 0 0)]
          (reset! start-time
                  (time/to-string
                    new-start-time
                    time/transit-format)))))

    [:div {:class "opux-card-filter__field opux-card-filter__field--datetime"}
     [:div {:class "opux-card-filter__label"}
      (tr [:pages.quest.list.filter.when])]
      (html/datepicker {:date start-time-atom
                        :position "bottom left"
                        :context context
                        :format time/date-print-format})]))

(defn- quest-filters
  [{:keys [tr cursors-and-schema context quest-filter]}]
  [:div {:class "opux-content opux-card-filter"}
   (quest-categories-filter {:context context
                             :cursors-and-schema cursors-and-schema
                             :categories (get-in cursors-and-schema [:categories :value])})

   (quest-location-filter {:context context
                           :cursors-and-schema cursors-and-schema})
   (quest-start-time-filter {:context context
                             :cursors-and-schema cursors-and-schema})])

(defn- monthly-quest-list
  [{:keys [quests context]}]
  (let [start-time (time/from-string (:start-time (first quests)))
        month-name (time/to-string start-time time/month-name-format)]
    [:div
     [:h2 {:class "opux-centered"}
      month-name]

     [:ul {:class "opux-card-list"}
      (map #(quest-card-browse {:context context
                                :quest %})
           quests)]]))

(rum/defc list-quests < rum/reactive
  [{:keys [context quests quest-filter schema errors filtered-quests]}]
  (let [tr (:tr context)
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
                (filters {:quests quests
                          :quest-filter new-filter}))))

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

        (map #(monthly-quest-list
                {:quests (quests-by-months %)
                 :context context})
             (reverse
               (sort
                 (keys quests-by-months)))))]]]))
