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

(rum/defcs quest-categories-filter < rum/reactive
  (rum/local false ::is-active)
  [state {:keys [cursors-and-schema context tr]}]
  (let [is-active (::is-active state)]
    [:div {:class "opux-card-filter__field opux-card-filter__field--category"}
     [:div {:class "opux-card-filter__label"}
      (tr [:pages.quest.list.filter.category])]
     [:span {:class "opux-icon opux-icon-plus opux-card-filter--category-filter"}]
     (html/form-section
      ""
      (html/multi-selector-for-schema
       {:schema (get-in cursors-and-schema [:categories :schema])
        :value (get-in cursors-and-schema [:categories :value])
        :error (get-in cursors-and-schema [:categories :error])
        :context context}))]))

(rum/defc quest-filters [{:keys [tr cursors-and-schema context]}]
  [:div {:class "opux-content opux-card-filter"}
   (quest-categories-filter {:context context
                             :tr tr
                             :cursors-and-schema cursors-and-schema})

   [:div {:class "opux-card-filter__field opex-card-filter__field--datetime"}
    [:div {:class "opux-card-filter__label"}
     (tr [:pages.quest.list.filter.where])]
    (html/location-selector
     {:class "opux-input opux-input--location-selector"
      :location (get-in cursors-and-schema [:location :value])
      :error (get-in cursors-and-schema [:location :error])
      :schema (get-in cursors-and-schema [:location :schema])
      :placeholder (tr [:pages.quest.edit.location.placeholder])
      :context context})]

   [:div {:class "opux-card-filter__field opex-card-filter__field--datetime"}
    [:div {:class "opux-card-filter__label"}
     (tr [:pages.quest.list.filter.when])]]])

(rum/defc list-quests
  [{:keys [context quests quest-filter schema errors]}]
  (let [tr (:tr context)
        cursors-and-schema
        (c/value-and-error-cursors-and-schema {:for quest-filter
                                               :schema schema
                                               :errors errors})]
    [:div {:class "opux-section"}
     [:h1 {:class "opux-centered"}
      (tr [:pages.quest.list.title])]

     (quest-filters {:cursors-and-schema cursors-and-schema
                     :tr tr
                     :context context})

     [:div {:class "opux-card-list-container"}
      [:div
       {:class "opux-content opux-content--small opux-centered opux-card-list__subtitle"}
       [:p (tr [:pages.quest.list.not-found])]]

      [:h2 {:class "opux-centered"}
       "Helmikuussa"]

      [:ul {:class "opux-card-list"}
       (repeat 7 (quest-card-browse))]

      [:h2 {:class "opux-centered"}
       "Maaliskuussa "]

      [:ul {:class "opux-card-list"}
       (repeat 8 (quest-card-browse))]
      ]]))
