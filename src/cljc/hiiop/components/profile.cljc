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

(rum/defcs quest-card-action-delete < rum/reactive
  (rum/local false ::processing)
  [state {:keys [quest card-state tr quests]}]
  (let [reset-card-state #(reset! card-state "default")
        processing (::processing state)]
    (if (rum/react processing)
      [:div {:class "opux-card__overlay"}
       [:div {:class "opux-content opux-centered"}
        [:i {:class "opux-icon opux-icon-ellipses"}]]]

      [:div {:class "opux-card__overlay"}
       [:div {:class "opux-content opux-centered"}
        (tr [:pages.profile.do-you-want-to-delete])]

       [:span
        {:class "opux-button opux-button--highlight opux-button--spacing"
         :on-click (fn [e]
                     (reset! processing true)
                     #?(:cljs
                        (go
                          (let [resp (<! (api/delete-quest (:id quest)))
                                new-quests (<! (api/get-own-quests))]
                            (reset! quests new-quests)
                            (reset! processing false)
                            (reset-card-state)))
                        ))}
        (tr [:pages.profile.delete])]

       [:span
        {:class "opux-button opux-button--dull opux-button--spacing"
         :on-click reset-card-state}
        (tr [:pages.profile.cancel])]
       ]
      )
    ))

(rum/defcs quest-card < rum/reactive
  (rum/local "default" ::card-state)
  [state {:keys [quest context quests]}]
  (let [{:keys [name
                location
                id
                start-time
                end-time
                picture-url
                max-participants]} quest
        quest-link (path-for hierarchy :quest :quest-id id)
        town (:town location)
        card-state (::card-state state)
        tr (:tr context)]

    [:div {:class "opux-card-container"}
     [:div {:class "opux-card opux-card--with-actions"}

      [:div {:class "opux-card__image-container"}
       [:div {:class "opux-card__status"}
        (tr [:pages.profile.my-event])]
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
        (str
         (time/to-string
          (time/from-string start-time) time/hour-minute-format)
         "-"
         (time/to-string
          (time/from-string end-time) time/hour-minute-format)
         )]

       [:div {:class "opux-card__actions"}
        [:span
         {:class "opux-card-action opux-icon-circled opux-icon-trashcan"
          :on-click (fn [e]
                      (if (not (= @card-state "delete"))
                        (reset! card-state "delete")))}]
        [:span {:class "opux-card-action opux-icon-circled opux-icon-personnel"}]
        [:a {:class "opux-card-action opux-icon-circled opux-icon-edit"
             :href (path-for hierarchy :edit-quest :quest-id (:id quest))}]]

       (if (= @card-state "delete")
         (quest-card-action-delete {:quest quest
                                    :card-state card-state
                                    :quests quests
                                    :tr tr}))
       ]]]))

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
         (map #(quest-card {:quest %
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
         (map #(quest-card {:quest %
                            :context context
                            :quests quests})
              past-quests)])
      ]]))
