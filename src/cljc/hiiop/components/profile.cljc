(ns hiiop.components.profile
  #?(:cljs
     (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require [clojure.string :as str]
            [rum.core :as rum]
            [taoensso.timbre :as log]
            [bidi.bidi :refer [path-for]]
            [schema.core :as s]
            [hiiop.routes.page-hierarchy :refer [hierarchy]]
            [schema.coerce :as sc]
            #?(:cljs [cljs.core.async :refer [<!]])
            #?(:cljs [hiiop.client-api :as api])
            [hiiop.time :as time]
            [hiiop.components.core :as c]
            [hiiop.html :as html]
            [hiiop.components.quest-card :refer [quest-card-profile]]
            [hiiop.schema :as hs]))
(defn get-past-quests
  [quests]
  (let [today (time/today)]
    (filter (fn [quest]
              (time/before? (time/from-string (:end-time quest)) today))
            quests)))

(defn get-upcoming-quests
  [quests]
  (let [today (time/today)]
    (filter (fn [quest]
              (time/after? (time/from-string (:end-time quest)) today))
            quests)))

(rum/defc profile < rum/reactive
  [{:keys [context user-info quests]}]
  (let [{:keys [email name moderator]} user-info
        tr (:tr context)
        past-quests (get-past-quests (rum/react quests))
        upcoming-quests (get-upcoming-quests (rum/react quests))]

    [:div {:class "opux-section opux-section--profile"}

     [:div {:class "opux-content opux-content--small"}
      [:h1 {:class "opux-centered"} name]
      [:h3 {:class "opux-centered"} email]

      [:div {:class "opux-section opux-centered"}
       [:span
        {:class "opux-button opux-button--spacing opux-button--dull"
         :on-click (fn [e]
                     #?(:cljs
                        (go
                          (let [result (<! (api/logout))]
                            (set! (.-location js/window)
                                  (path-for hierarchy :login))))))}
        (tr [:actions.profile.sign-out])]
        
       [:a {:class "opux-button opux-button--spacing opux-button--highlight"
            :href (path-for hierarchy :edit-profile)}
        (tr [:actions.profile.edit])]

       (if moderator
         [:a {:class "opux-button opux-button--spacing opux-button--highlight"
              :href (path-for hierarchy :moderate)}
          (tr [:pages.profile.moderation])])]]

     [:div {:class "opux-card-list-container"}
      [:div {:class "opux-content"}

       [:h2 {:class "opux-centered"}
        (tr [:pages.profile.upcoming-quests])]

       (if (not (nil? upcoming-quests))
         [:ul {:class "opux-card-list opux-card-list--centered"}
          (map #(quest-card-profile {:quest %
                                     :context context
                                     :quests quests})
               upcoming-quests)]
         [:h3 {:class "opux-centered"}
          (tr [:pages.profile.no-upcoming-quests])]
         )

       (if (not-empty past-quests)
         [:h2 {:class "opux-centered"}
          (tr [:pages.profile.past-quests])])
       (if (not-empty past-quests)
         [:ul {:class "opux-card-list"}
          (map #(quest-card-profile {:quest %
                                     :context context
                                     :quests quests})
               past-quests)])
       ]]]))

(rum/defc edit-profile < rum/reactive
  [{:keys [context user-info user-edit schema errors]}]
  (let [{:keys [name phone]} user-info
        tr (:tr context)
        cursors-and-schema (c/value-and-error-cursors-and-schema
                            {:for user-edit
                             :schema schema
                             :errors errors})]
    [:form
     {:class "opux-form"
      :on-submit
      (fn [e]
        (.preventDefault e)
        (println @user-edit)
        #?(:cljs
           (go
             (<! (api/edit-user (:id user-info) @user-edit)))))}
     
     [:div {:class "opux-form-section"}
      [:div {:class "opux-content opux-content--small"}
      [:div {:class "opux-fieldset opux-form-section__fieldset"}
       
       [:div {:class "opux-fieldset__item"}
        (html/label
         (tr [:pages.edit-profile.name])
         {:class "opux-input__label name-label"})
        (html/input
         {:schema (get-in cursors-and-schema [:name :schema])
          :value (get-in cursors-and-schema [:name :value])
          :error (atom nil)
          :type "text"
          :class "opux-input opux-input--text name"
          :context context})]

       [:div {:class "opux-fieldset__item"}
        (html/label
         (tr [:pages.edit-profile.phone])
         {:class "opux-input__label phone-label"})
        (html/input
         {:schema (get-in cursors-and-schema [:phone :schema])
          :value (get-in cursors-and-schema [:phone :value])
          :error (atom nil)
          :type "text"
          :class "opux-input opux-input--text phone"
          :context context})]

       [:div {:class "opux-section opux-centered"}
        [:a {:class "opux-button opux-button--spacing opux-button--dull"
             :href (path-for hierarchy :profile)}
        (tr [:actions.profile.cancel-editing])]

       (html/button
        (tr [:actions.profile.save-profile])
        {:class "opux-button opux-button--spacing opux-button--highlight"
         :type "submit"
         :active (atom true)}
       )]]]]]))
