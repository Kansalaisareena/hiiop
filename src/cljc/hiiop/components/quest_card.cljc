(ns hiiop.components.quest-card
  #?(:cljs
     (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require [clojure.string :as str]
            [rum.core :as rum]
            [bidi.bidi :refer [path-for]]
            [hiiop.routes.page-hierarchy :refer [hierarchy]]
            [hiiop.time :as time]
            #?(:cljs [cljs.core.async :refer [<!]])
            #?(:cljs [hiiop.client-api :as api])))

(defn get-quest-image [quest]
  (let [{:keys [picture-url categories]} quest]
    (if (nil? picture-url)
      (str "/images/category/"
          (name (first categories))
          ".jpg")
      picture-url)))

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

(rum/defcs quest-card-profile < rum/reactive
  (rum/local "default" ::card-state)
  [state {:keys [quest context quests]}]
  (let [card-state (::card-state state)
        {:keys [name
                location
                id
                start-time
                end-time
                picture-url
                max-participants]} quest
        quest-link (path-for hierarchy :quest :quest-id id)
        town (:town location)
        is-own-quest (= (str (:id (:identity context)))
                        (str (:owner quest)))
        is-open (:is-open quest)
        tr (:tr context)]

    [:div {:class "opux-card-container"}
     [:div {:class "opux-card opux-card--with-actions"}

      [:div {:class "opux-card__image-container"}
       [:div {:class "opux-card__status"}
        (if is-own-quest
          (str (tr [:pages.profile.my-event])
               " | "
               (if (not is-open)
                 (tr [:pages.profile.pending-approval])
                 (tr [:pages.profile.published]))))]
       [:a {:href quest-link}
        [:div {:class "opux-card__image"
               :style {:background-image (str "url('"
                                              (get-quest-image quest)
                                              "')")}}]]]

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

       (if is-own-quest
         [:div {:class "opux-card__actions"}
          [:span
           {:class "opux-card-action opux-icon-circled opux-icon-trashcan"
            :on-click (fn [e]
                        (if (not (= @card-state "delete"))
                          (reset! card-state "delete")))}]

          (if is-open
            [:span {:class "opux-card-action opux-icon-circled opux-icon-personnel"}])

          [:a {:class "opux-card-action opux-icon-circled opux-icon-edit"
               :href (path-for hierarchy :edit-quest :quest-id (:id quest))}]

          (if (= @card-state "delete")
            (quest-card-action-delete {:quest quest
                                       :card-state card-state
                                       :quests quests
                                       :tr tr}))]

         [:div {:class "opux-card__actions"}
          [:span {:class "opux-button"}
           (tr [:pages.profile.cancel-enrollment])]])]]]))

(rum/defc quest-card-browse [{:keys [quest context]}]
  (let [{:keys [name
                location
                id
                start-time
                end-time
                picture-url
                max-participants]} quest
        quest-link (path-for hierarchy :quest :quest-id id)
        town (:town location)
        tr (:tr context)]

    [:div {:class "opux-card-container"}
     [:div {:class "opux-card"}

      [:div {:class "opux-card__image-container"}
       [:a {:href quest-link}
        [:div {:class "opux-card__image"
               :style {:background-image (str "url('"
                                              (get-quest-image quest)
                                              "')")}}]]]

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
       ]]]))
