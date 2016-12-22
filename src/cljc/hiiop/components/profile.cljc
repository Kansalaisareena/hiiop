(ns hiiop.components.profile
  #?(:cljs
     (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require [clojure.string :as str]
            [rum.core :as rum]
            [taoensso.timbre :as log]
            [schema.core :as s]
            [schema.coerce :as sc]
            #?(:cljs [cljs.core.async :refer [<!]])
            #?(:cljs [hiiop.client-api :as api])
            [hiiop.time :as time]
            [hiiop.components.core :as c]
            [hiiop.html :as html]
            [hiiop.schema :as hs]))

(rum/defc card []
  [:div {:class "opux-card-container"}
   [:div {:class "opux-card"}

    [:div {:class "opux-card__image-container"}
     [:div {:class "opux-card__status"}
      "Oma tapahtuma | Odottaa hyväksymistä"]
     [:a {:href "#"}
      [:img {:class "opux-card__image"
             :src "https://placeholdit.imgix.net/~text?txtsize=33&txt=quest%20image&w=480&h=300"}]]]

    [:div {:class "opux-card__content"}

     [:span {:class "opux-card__location opux-inline-icon opux-inline-icon-location"}
      "Helsinki"]
     [:span {:class "opux-card__attendance opux-inline-icon opux-inline-icon-personnel opux-inline-icon--right"}
      23]

     [:a {:class "opux-card__title" :href "#"}
      "Konalan kehitysvammaisten iltatanhutapahtuma"]

     [:span {:class "opux-card__date opux-inline-icon opux-inline-icon-calendar"}
      "Keskiviikko 28.1"]
     [:span {:class "opux-card__time opux-inline-icon opux-inline-icon-clock"}
      "18.00-20.00"]

     [:div {:class "opux-card__actions"}
      [:span {:class "opux-card-action opux-icon-circled opux-icon-trashcan"}]
      [:span {:class "opux-card-action opux-icon-circled opux-icon-personnel"}]
      [:span {:class "opux-card-action opux-icon-circled opux-icon-edit"}]
      ]
     ]]])

(rum/defc profile [{:keys [context quests user-info]}]
  (let [{:keys [email name]} user-info
        tr (:tr context)]

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
       (tr [:pages.profile.attending])]

      [:ul {:class "opux-card-list opux-card-list--centered"}
       (repeat 2 (card))]

      [:h2 {:class "opux-centered"}
       (tr [:pages.profile.submissions])]

      [:ul {:class "opux-card-list"}
       (repeat 5 (card))]
      ]]))
