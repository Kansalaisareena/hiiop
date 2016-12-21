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

(rum/defcs profile < rum/reactive
  (rum/local "" ::email)
  (rum/local "" ::name)
  (rum/local false ::processed)
  [state {:keys [context quests]}]
  (let [email (::email state)
        name (::name state)
        processed (::processed state)
        tr (:tr context)]

    #?(:cljs
       (when-not (rum/react processed)
         (go
           (let [user-info (<! (api/get-user-info (:id (:identity context))))]
             (when (nil? (:errors user-info))
               (reset! processed true)
               (reset! email (:email user-info))
               (reset! name (:name user-info)))))))

    [:div {:class "opux-section opux-section--profile"}

     [:div {:class "opux-content opux-content--small"}
      [:h1 (rum/react name)]
      [:h3 (rum/react email)]

      [:div {:class "opux-section opux-centered"}
       [:span {:class "opux-button opux-button--spacing opux-button--dull"}
        (tr [:pages.profile.sign-out])]
       [:span {:class "opux-button opux-button--spacing opux-button--highlight"}
        (tr [:pages.profile.edit])]]]

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
