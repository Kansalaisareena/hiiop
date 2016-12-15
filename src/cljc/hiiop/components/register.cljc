(ns hiiop.components.register
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require
   [rum.core :as rum]
   [schema.core :as s]
   [hiiop.schema :as schema]
   [taoensso.timbre :as log]
   [bidi.bidi :refer [path-for]]
   #?(:cljs [cljs.core.async :refer [<!]])
   #?(:cljs [hiiop.client-api :as api])
   [hiiop.html :as html]
   [hiiop.routes.page-hierarchy :as pages]))

(rum/defc registered-notice [context]
  (let [tr (:tr context)]
    [:div {:class "opux-content opux-content--small opux-centered"}
     [:h2 (tr [:page.register.check-your-email])]
     [:p (tr [:page.register.email-sent-message])]]))

(rum/defcs register < (rum/local false ::registered)
  [state {:keys [context]}]
  (let [registered (::registered state)
        tr (:tr context)
        credentials (atom {:email "" :name ""})
        name (rum/cursor credentials :name)
        email (rum/cursor credentials :email)]

    (if @registered
      (registered-notice context)
      [:form
       {:class "opux-form"
        :on-submit
        (fn [e]
          (.preventDefault e)
          #?(:cljs
             (go
               (let [success (<! (api/register @credentials))]
                 (if success
                   (swap! registered not))))))}

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
           :value (tr [:actions.user.register])}]]]])))
