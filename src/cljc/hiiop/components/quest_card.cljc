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
      (str "/img/category/"
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
                                user-quests (<! (api/get-user-quests))
                                new-own-quests (:organizing user-quests)
                                new-participating-quests (:attending user-quests)
                                new-quests (into [] (distinct (concat new-own-quests new-participating-quests)))]
                            (reset! quests new-quests)
                            (reset! processing false)
                            (reset-card-state)))))}
        (tr [:pages.profile.delete])]

       [:span
        {:class "opux-button opux-button--dull opux-button--spacing"
         :on-click reset-card-state}
        (tr [:pages.profile.cancel])]])))

(rum/defcs quest-card-action-cancel-enrollment < rum/reactive
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
        (tr [:pages.profile.do-you-want-to-cancel-enrollment])]

       [:span
        {:class "opux-button opux-button--highlight opux-button--spacing"
         :on-click (fn [e]
                     (reset! processing true)
                     #?(:cljs
                        (go
                          (let [party-info (first (<! (api/get-party-info {:quest-id (:id quest)})))
                                resp (<! (api/remove-party-member
                                           {:member-id (:member-id party-info)
                                            :quest-id (:id quest)}))
                                user-quests (<! (api/get-user-quests))
                                new-own-quests (:organizing user-quests)
                                new-participating-quests (:attending user-quests)
                                new-quests (into [] (distinct (concat new-own-quests new-participating-quests)))]
                            (reset! quests new-quests)
                            (reset! processing false)
                            (reset-card-state)))))}
        (tr [:pages.profile.cancel-enrollment])]

       [:span
        {:class "opux-button opux-button--dull opux-button--spacing"
         :on-click reset-card-state}
        (tr [:pages.profile.cancel])]])))

(defn- quest-card-image [{:keys [quest]}]
  (let [quest-image (get-quest-image quest)
        quest-id (:id quest)
        quest-link (path-for hierarchy :quest :quest-id quest-id)
        moderated (:moderated quest)]
    (if moderated
      [:a {:href quest-link}
       [:div {:class "opux-card__image"
              :style {:background-image (str "url('" quest-image "')")}}]]

      [:div {:class "opux-card__image"
             :style {:background-image (str "url('" quest-image "')")}}])))

(defn- quest-card-title [{:keys [quest]}]
  (let [quest-id (:id quest)
        name (:name quest)
        quest-link (path-for hierarchy :quest :quest-id quest-id)
        moderated (:moderated quest)]

    (if moderated 
      [:a {:class "opux-card__title" :href quest-link} name]
      [:div {:class "opux-card__title"} name])))

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
                participant-count
                max-participants]} quest
        available-slots (- max-participants participant-count)
        quest-link (path-for hierarchy :quest :quest-id id)
        town (:town location)
        is-own-quest (= (str (:id (:identity context)))
                        (str (:owner quest)))
        is-open (:is-open quest)
        moderated (:moderated quest)
        tr (:tr context)]

    [:div {:class "opux-card-container"}
     [:div {:class "opux-card opux-card--with-actions"}

      [:div {:class "opux-card__image-container"}
       [:div {:class "opux-card__status"}
        (if is-own-quest
          (str (tr [:pages.profile.my-event])
               " | "
               (if (not moderated)
                 (tr [:pages.profile.pending-approval])
                 (tr [:pages.profile.published]))))]
       (quest-card-image {:quest quest})]

      [:div {:class "opux-card__content"}

       [:span
        {:class "opux-card__location opux-inline-icon opux-inline-icon-location"}
        town]
       [:span
        {:class "opux-card__attendance opux-inline-icon opux-inline-icon-personnel opux-inline-icon--right"}
        available-slots]

       (quest-card-title {:quest quest})

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

       (cond (= @card-state "delete")
             (quest-card-action-delete
               {:quest quest
                :card-state card-state
                :quests quests
                :tr tr})

             (= @card-state "cancel-enrollment")
             (quest-card-action-cancel-enrollment
               {:quest quest
                :card-state card-state
                :quests quests
                :tr tr}))

       (if is-own-quest

         ;; own quest
         (when moderated
           [:div {:class "opux-card__actions"}
            [:span
             {:class "opux-card-action opux-icon-circled opux-icon-trashcan"
              :on-click #(if (not (= @card-state "delete"))
                           (reset! card-state "delete"))}]

            [:span {:class "opux-card-action opux-icon-circled opux-icon-personnel"}]

            [:a {:class "opux-card-action opux-icon-circled opux-icon-edit"
                 :href (path-for hierarchy :edit-quest :quest-id (:id quest))}]])

         ;; participating quest
         [:div {:class "opux-card__actions"} 
          [:span {:class "opux-button"
                  :on-click #(if (not (= @card-state "cancel-enrollment"))
                               (reset! card-state "cancel-enrollment"))}
           (tr [:pages.profile.cancel-enrollment])]])]]]))

(rum/defc quest-card-browse [{:keys [quest context]}]
  (let [{:keys [name
                location
                id
                start-time
                end-time
                picture-url
                participant-count
                max-participants]} quest
        available-slots (- max-participants participant-count)
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
        available-slots]

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
            (time/from-string end-time) time/hour-minute-format))]]]]))

(rum/defc quest-card-moderate [{:keys [quest context is-moderated on-click-fn]}]
  (let [{:keys [name
                location
                id
                start-time
                end-time
                picture-url
                participant-count
                max-participants]} quest
        available-slots (- max-participants participant-count)
        quest-link (path-for hierarchy :quest :quest-id id)
        town (:town location)
        tr (:tr context)]

    [:div {:class "opux-card-container"}
     [:div {:class "opux-card"}

      [:div {:class "opux-card__image-container"}
       (if is-moderated
         [:a {:href quest-link}
          [:div {:class "opux-card__image"
                 :style {:background-image (str "url('" (get-quest-image quest) "')")}}]]

         [:div {:class "opux-card__image"
                :style {:background-image (str "url('" (get-quest-image quest) "')")}
                :on-click on-click-fn}])]

      [:div {:class "opux-card__content"}

       [:span
        {:class "opux-card__location opux-inline-icon opux-inline-icon-location"}
        town]
       [:span
        {:class "opux-card__attendance opux-inline-icon opux-inline-icon-personnel opux-inline-icon--right"}
        available-slots]

       (if is-moderated
         [:a {:class "opux-card__title" :href quest-link} name]
         [:div {:class "opux-card__title"
                :on-click on-click-fn}
          name])

       [:span {:class "opux-card__date opux-inline-icon opux-inline-icon-calendar"}
        #?(:cljs
           (time/to-string (time/from-string start-time) time/with-weekday-format)
           :clj
           (time/to-string start-time time/with-weekday-format)
           )]

       (if (not (nil? end-time))
         [:span {:class "opux-card__time opux-inline-icon opux-inline-icon-clock"}
          #?(:cljs
             (time/to-string (time/from-string end-time) time/with-weekday-format)
             :clj
             (time/to-string end-time time/with-weekday-format))])]]]))
