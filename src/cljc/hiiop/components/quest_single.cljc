(ns hiiop.components.quest-single
  #?(:cljs
     (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require [clojure.string :as string]
            [rum.core :as rum]
            [hiiop.html :as html]
            [hiiop.time :as time]
            [hiiop.components.quest-signup-form :refer [signup-form]]
            [taoensso.timbre :as log]))

(rum/defc quest < rum/reactive
  [{:keys [context quest quest-signup-info errors schema]}]
  (let [{:keys [name
                organisation
                owner-name
                location
                picture-url
                hashtags
                max-participants
                start-time
                description]} @quest
        {:keys [street-number
                street
                town
                postal-code
                country
                google-maps-url]} location
        tr (:tr context)]

    [:div {:class "opux-section"}

     [:div {:class "opux-content opux-content--quest-image-header"
            :style {:background-image (str "url('" picture-url "')")}}]

     [:div {:class "opux-content opux-content--medium"}
      [:h1 name]]

     [:div {:class "opux-content opux-content--medium opux-content--quest-header"}
      [:p
       [:i {:class "opux-icon opux-icon-person"}]
       (html/combine-text ", " owner-name (:name organisation))]
      [:p
       [:i {:class "opux-icon opux-icon-location"}]
       (html/combine-text ", " street-number street town postal-code)]
      [:p
       [:i {:class "opux-icon opux-icon-calendar"}]
       (time/to-string (time/from-string start-time) time/date-print-format)]]

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
       (str max-participants " " (tr [:pages.quest.participants]))]]

     [:div {:class "opux-line"}]

     (when quest-signup-info
       (signup-form {:context context
                     :quest-signup-info quest-signup-info
                     :schema schema
                     :errors errors}))]))
