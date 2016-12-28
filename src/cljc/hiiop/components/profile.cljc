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
  (let [{:keys [email name]} user-info
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

       [:span {:class "opux-button opux-button--spacing opux-button--highlight"}
        (tr [:actions.profile.edit])]]]

     [:div {:class "opux-card-list-container"}

      [:h2 {:class "opux-centered"}
       (tr [:pages.profile.upcoming-quests])]

      (if (not-empty upcoming-quests)
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
      ]]))
