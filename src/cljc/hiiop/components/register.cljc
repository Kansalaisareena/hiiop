(ns hiiop.components.register
  (:require
   [rum.core :as rum]
   [schema.core :as s]
   [hiiop.schema :as schema]
   [taoensso.timbre :as log]
   [bidi.bidi :refer [path-for]]
   [hiiop.html :as html]
   [hiiop.routes.page-hierarchy :as pages]))

(rum/defc register [{:keys [context]}]
  (let [tr (:tr context)
        credentials (atom {:email "" :name ""})
        name (rum/cursor credentials :name)
        email (rum/cursor credentials :email)]
    [:form {:class "opux-form"
            :on-submit
            (fn [e]
              (.preventDefault e))}
     [:div {:class "opux-form-section"}
      [:h2 (tr [:pages.register.title])]
      [:div {:class "opux-fieldset opux-form-section__fieldset"}
       [:div {:class "opux-fieldset__item"}
        (html/label
         (tr [:pages.register.name])
         {:class "opux-input__label name-label"})
        (html/input
         {:schema s/Str
          :value name
          :type "text"
          :class "opux-input opux-input--text name"
          :error (atom nil)})]

       [:div {:class "opux-fieldset__item"}
        (html/label
         (tr [:pages.register.email])
         {:class "opux-input__label name-label"})
        (html/input
         {:schema s/Str
          :value email
          :type "text"
          :class "opux-input opux-input--text email"
          :error (atom nil)})]

       [:input
        {:class "opux-button"
         :type "submit"
         :value (tr [:actions.user.register])}]
       ]]]))
