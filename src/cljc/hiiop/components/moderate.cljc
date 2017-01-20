(ns hiiop.components.moderate
  #?(:cljs
     (:require-macros [cljs.core.async.macros :refer [go]])
     )
  (:require [rum.core :as rum]
            [clojure.string :as string]
            [taoensso.timbre :as log]
            #?(:cljs [hiiop.client-api :refer [get-private-user-info
                                               moderate-quest
                                               reject-quest
                                               get-unmoderated-quests]])
            #?(:cljs [cljs.core.async :refer [<!]])
            [hiiop.html :as html]
            [hiiop.time :as time]
            [hiiop.components.quest-card :refer [get-quest-image quest-card-moderate]]))

(rum/defcs quest-overlay < rum/reactive
                           (rum/local nil ::usable-owner)
                           (rum/local false ::processing)
  [state {:keys [context quest on-close-fn quests]}]
  (let [usable-owner (::usable-owner state)
        processing (::processing state)
        message (atom "")
        {:keys [name
                organisation
                owner-name
                location
                picture-url
                hashtags
                max-participants
                start-time
                end-time
                description]} quest
        {:keys [street-number
                street
                town
                postal-code
                country
                google-maps-url]} location
        tr (:tr context)]

    #?(:cljs
       (if (and (nil? @usable-owner) (nil? owner-name))
         (go
           (let [user (<! (get-private-user-info (:owner quest)))]
             (reset! usable-owner (:name user))))))

    (if @processing
      [:div {:class "opux-section"}
       [:div {:class "opux-content opux-centered"}
        [:i {:class "opux-icon opux-icon-ellipses"}]]]

      [:div {:class "opux-section"}
       [:div {:class "opux-content opux-content--small opux-content--image-header opux-content--image-header--small"
              :style {:background-image (str "url('" (get-quest-image quest) "')")}}]

       [:div {:class "opux-content opux-content--medium"}
        [:h1 name]]

       [:div {:class "opux-content opux-content--medium opux-content--quest-header"}
        [:p [:i {:class "opux-icon opux-icon-person"}]
         (html/combine-text ", " (rum/react usable-owner) (:name organisation))]
        [:p [:i {:class "opux-icon opux-icon-location"}]
         (html/combine-text ", " street-number street town postal-code)]
        [:p
         [:i {:class "opux-icon opux-icon-calendar"}]
         (time/duration-to-print-str-date
           (time/from-string start-time)
           (time/from-string end-time))]
        [:p
         [:i {:class "opux-icon opux-icon-clock"}]
         (time/duration-to-print-str-time
           (time/from-string start-time)
           (time/from-string end-time))]]

       [:div {:class "opux-content opux-content--medium"} (html/wrap-paragraph description)]

       (if (not (nil? organisation))
         [:div {:class "opux-content opux-content--medium"}
          [:h3 (:name organisation)]
          (if (not (nil? (:description organisation)))
            (html/wrap-paragraph (:description organisation)))])

       [:div {:class "opux-content opux-content--medium opux-content--quest-footer"}
        (if (not-empty hashtags)
          [:p (string/join " " hashtags)])
        [:p
         [:i {:class "opux-icon opux-icon-personnel"}]
         (str max-participants " " (tr [:pages.quest.view.participants]))]]

       [:div {:class "opux-moderate-overlay--control opux-centered"}
        [:h2 (tr [:pages.moderate.reject-quest-message-title])]
        [:textarea
         {:style {:width "80%"
                  :height "7rem"
                  :font-size "1rem"
                  :max-width "400px"}
          :on-change
          (fn [e]
            (reset! message (html/value-from-event e))
            )}]]

       [:div {:class "opux-moderate-overlay--control opux-centered opux-button--spacing"}
        [:div
         {:class "opux-button opux-button--dull opux-button--spacing"
          :on-click
          (fn []
            (swap! processing not)
            #?(:cljs
               (go
                 (let [result (<! (reject-quest
                                   {:quest-id (:id quest)
                                    :message @message }))]
                   (if (not (nil? result))
                     (let [new-quests (<! (get-unmoderated-quests))]
                       (reset! quests new-quests)
                       (on-close-fn)))))))}
         (tr [:pages.moderate.reject-quest])]

        [:div
         {:class "opux-button opux-button--dull opux-button--spacing"
          :on-click on-close-fn}
         (tr [:pages.moderate.cancel])]

        [:div
         {:class "opux-button opux-button--highlight opux-button--spacing"
          :on-click
          (fn []
            (swap! processing not)
            #?(:cljs
               (go
                 (let [result (<! (moderate-quest (:id quest)))]
                   (if (not (nil? result))
                     (let [new-quests (<! (get-unmoderated-quests))]
                       (reset! quests new-quests)
                       (on-close-fn))
                     )))))}
         (tr [:pages.moderate.moderate-quest])]]])))

(rum/defcs moderate-page < rum/reactive
                           (rum/local nil ::active-quest)
  [state {:keys [context unmoderated-quests]}]
  (let [tr (:tr context)
        active-quest (::active-quest state)]
    [:div {:class "opux-section"}
     [:h1 {:class "opux-centered"}
      (tr [:pages.moderate.title])]

     (if (nil? @active-quest)
       ;; show moderation list
       [:div {:class "opux-card-list-container"}
        [:div {:class "opux-content"}
         [:h2 {:class "opux-centered"}
          (tr [:pages.moderate.unmoderated-quests])]
         [:ul {:class "opux-card-list"}
          (map #(quest-card-moderate {:context context
                                      :quest %
                                      :is-moderated false
                                      :on-click-fn
                                      (fn []
                                        (reset! active-quest (:id %)))})
               @unmoderated-quests)]]]

       ;; else, show selected quest for moderation
       (let [quest (first
                    (filter
                     #(= @active-quest (:id %))
                     @unmoderated-quests))]
         (quest-overlay {:context context
                         :quest quest
                         :quests unmoderated-quests
                         :on-close-fn
                         (fn []
                           (reset! active-quest nil))})))
     ]))
