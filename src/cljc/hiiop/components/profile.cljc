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
            [hiiop.schema :as hs]))

(rum/defc quest-card [quest]
  (let [{:keys [name
                location
                id
                start-time
                picture-url
                max-participants]} quest
        quest-link (path-for hierarchy :quest :quest-id id)
        town (:town location)]

    [:div {:class "opux-card-container"}
     [:div {:class "opux-card"}

      [:div {:class "opux-card__image-container"}
       [:div {:class "opux-card__status"}
        "Oma tapahtuma | Odottaa hyväksymistä"]
       [:a {:href quest-link}
        [:div {:class "opux-card__image"
               :style {:background-image (str "url('" (or picture-url "https://placeholdit.imgix.net/~text?txtsize=33&txt=quest%20image&w=480&h=300") "')")}}]]]

      [:div {:class "opux-card__content"}

       [:span
        {:class "opux-card__location opux-inline-icon opux-inline-icon-location"}
        town]
       [:span
        {:class "opux-card__attendance opux-inline-icon opux-inline-icon-personnel opux-inline-icon--right"}
        max-participants]

       [:a {:class "opux-card__title" :href quest-link}
        name]

       [:span {:class "opux-card__date opux-inline-icon opux-inline-icon-calendar"}
        (time/to-string (time/from-string start-time) time/with-weekday-format)]
       [:span {:class "opux-card__time opux-inline-icon opux-inline-icon-clock"}
        (time/to-string (time/from-string start-time) time/hour-minute-format)]

       [:div {:class "opux-card__actions"}
        [:span {:class "opux-card-action opux-icon-circled opux-icon-trashcan"}]
        [:span {:class "opux-card-action opux-icon-circled opux-icon-personnel"}]
        [:span {:class "opux-card-action opux-icon-circled opux-icon-edit"}]
        ]]]]))

(defn get-past-quests
  [quests]
  (let [today (time/today)]
    (filter (fn [quest]
              (time/before? (time/from-string (:end-time quest)) today))
            quests)))

(defn get-upcomping-quests
  [quests]
  (let [today (time/today)]
    (filter (fn [quest]
              (time/after? (time/from-string (:start-time quest)) today))
            quests)))

(rum/defc profile
  [{:keys [context quests user-info]}]
  (let [{:keys [email name]} user-info
        tr (:tr context)
        past-quests (get-past-quests quests)
        upcoming-quests (get-upcomping-quests quests)]

    [:div {:class "opux-section opux-section--profile"}

     [:div {:class "opux-content opux-content--small"}
      [:h1 {:class "opux-centered"} name]
      [:h3 {:class "opux-centered"} email]

      [:div {:class "opux-section opux-centered"}
       [:span {:class "opux-button opux-button--spacing opux-button--dull"}
        (tr [:actions.profile.sign-out])]
       [:span {:class "opux-button opux-button--spacing opux-button--highlight"}
        (tr [:actions.profile.edit])]]]

     [:div {:class "opux-card-list-container"}

      [:h2 {:class "opux-centered"}
       (tr [:pages.profile.upcoming-quests])]

      (if (not-empty upcoming-quests)
        [:ul {:class "opux-card-list opux-card-list--centered"}
         (map #(quest-card %) upcoming-quests)]
        [:h3 "You have no upcoming quests"]
        )

      (if (not-empty past-quests)
        [:h2 {:class "opux-centered"}
         (tr [:pages.profile.past-quests])])
      (if (not-empty past-quests)
        [:ul {:class "opux-card-list"}
         (map #(quest-card %) past-quests)])
      ]]))
