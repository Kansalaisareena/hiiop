(ns hiiop.components.activate
 (:require
   [rum.core :as rum]
   [schema.core :as s]
   [hiiop.schema :as schema]
   [hiiop.components.core :as c]
   [taoensso.timbre :as log]
   [bidi.bidi :refer [path-for]]
   [hiiop.html :as html]
   [hiiop.routes.page-hierarchy :as pages]))

(rum/defc activate [{:keys [context token]}]
  (let [tr (:tr context)
        credentials (atom {:password "" :confirm-password ""})
        password (rum/cursor credentials :password)
        confirm-password (rum/cursor credentials :confirm-password)]
    [:form {:class "opux-form"
            :on-submit
            (fn [e]
              (.preventDefault e))}
     [:div {:class "opux-form-section"}
      [:h2 (tr [:pages.activate.title])]
      [:div {:class "opux-fieldset opux-form-section__fieldset"}
       [:div {:class "opux-fieldset__item"}
        (html/label
         (tr [:pages.activate.password])
         {:class "opux-input__label password-label"})
        (html/input
         {:schema s/Str
          :value password
          :type "password"
          :class "opux-input opux-input--text password"
          :error (atom nil)})]

       [:div {:class "opux-fieldset__item"}
        (html/label
         (tr [:pages.activate.confirm-password])
         {:class "opux-input__label password-label"})
        (html/input
         {:schema s/Str
          :value confirm-password
          :type "password"
          :class "opux-input opux-input--text confirm-password"
          :error (atom nil)})]

       [:input
        {:class "opux-button"
         :type "submit"
         :value (tr [:actions.user.save-password])}]
       ]]]))
