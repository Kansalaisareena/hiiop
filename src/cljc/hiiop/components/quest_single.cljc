(ns hiiop.components.quest-single
  #?(:cljs
     (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require [clojure.string :as string]
            [rum.core :as rum]
            [hiiop.html :as html]
            [hiiop.time :as time]
            [hiiop.components.join-quest :refer [join-quest]]
            [hiiop.components.quest-card :refer [get-quest-image]]
            [taoensso.timbre :as log]))

(rum/defc quest < rum/reactive
  [{:keys [context quest user empty-party-member party-member-errors party-member-schema secret-party joinable]}]
  (let [{:keys [name
                organisation
                owner-name
                location
                picture-url
                hashtags
                max-participants
                participant-count
                start-time
                end-time
                description]} @quest
        {:keys [street-number
                street
                town
                postal-code
                country
                google-maps-url]} location
        available-slots (- max-participants participant-count)
        usable-owner (or owner-name (:name user))
        tr (:tr context)
        days-between (time/days-between
                       (time/from-string start-time)
                       (time/from-string end-time))]

    [:div {:class "opux-section"}

     [:div {:class "opux-content opux-content--image-header"
            :style {:background-image (str "url('"
                                           (get-quest-image @quest)
                                           "')")}}]

     [:div {:class "opux-content opux-content--medium"}
      [:h1 name]]

     [:div {:class "opux-content opux-content--medium opux-content--quest-header"}
      [:p
       [:i {:class "opux-icon opux-icon-person"}]
       (html/combine-text ", " usable-owner (:name organisation))]
      [:p
       [:i {:class "opux-icon opux-icon-location"}]
       (html/combine-text
         ", "
         (str street " " street-number)
         town postal-code)]
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
       (str available-slots " / " max-participants
            " " (tr [:pages.quest.view.participants]))]]

     [:div {:class "opux-content"}
      [:div {:class "opux-line"}]]

     (if joinable
       (cond
         (and (:is-open @quest) empty-party-member)
         (join-quest {:context context
                      :quest-id (:id @quest)
                      :party-member empty-party-member
                      :schema party-member-schema
                      :days-between days-between
                      :errors party-member-errors})

         (and (not (:is-open @quest)) secret-party)
         (join-quest {:context context
                      :quest-id (:id @quest)
                      :party-member empty-party-member
                      :schema party-member-schema
                      :days-between days-between
                      :errors party-member-errors
                      :secret-party secret-party})))]))
