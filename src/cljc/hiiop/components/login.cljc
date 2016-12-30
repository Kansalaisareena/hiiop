(ns hiiop.components.login
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require
   [rum.core :as rum]
   [schema.core :as s]
   [taoensso.timbre :as log]
   [bidi.bidi :refer [path-for]]
   #?(:cljs [cljs.core.async :refer [<!]])
   #?(:cljs [hiiop.client-api :as api])
   [hiiop.schema :as schema]
   [hiiop.html :as html]
   [hiiop.url :as u]
   [hiiop.routes.page-hierarchy :as pages]))

(rum/defc login [{:keys [context]}]
  (let [tr (:tr context)
        credentials (atom {:email "" :password ""})
        user (rum/cursor credentials :email)
        password (rum/cursor credentials :password)]
    [:form
     {:class "opux-form"
      :on-submit
      (fn [e]
        (.preventDefault e)
        #?(:cljs
           (go
             (let [status (<! (api/login @credentials))
                   sitten (keyword (:sitten (u/query-params (.-href js/location))))
                   to-key (or sitten :index)
                   to (or (path-for pages/hierarchy to-key) (path-for pages/hierarchy :index))]
               (when status
                 (set! (.-pathname js/location) to))))
           ))}
     [:div {:class "opux-form-section"}
      [:h2 (tr [:pages.login.title])]

      [:div {:class "opux-content opux-content--small opux-centered"}
       (tr [:pages.login.login-subtitle])]

      [:div {:class "opux-fieldset opux-form-section__fieldset"}

       [:div {:class "opux-fieldset__item"}
        (html/label
         (tr [:pages.login.email])
         {:class "opux-input__label user-label"})
        (html/input
         {:schema s/Str
          :value user
          :type "text"
          :class "opux-input opux-input--text email"
          :error (atom nil)})]

       [:div {:class "opux-fieldset opux-fieldset__item"}
        (html/label
         (tr [:pages.login.password])
         {:class "opux-input__label password-label"})
        (html/input
         {:schema s/Str
          :value password
          :type "password"
          :class "opux-input opux-input--text password"
          :error (atom nil)})]

       [:div {:class "opux-fieldset__inline-container opux-fieldset opux-fieldset__item opux-fieldset--login-links"}
        [:a {:class "opux-forget-password-link"
             :href "#"}
         (tr [:pages.login.forget-password])]
        [:a {:class "opux-register-link"
             :href (path-for pages/hierarchy :register)}
         (tr [:pages.login.register])]]

       [:input
        {:type "checkbox"
         :class "opux-input--checkbox"
         :default-checked false
         :id (name :pages.login.remember-me)}]
       (html/label
        (tr [:pages.login.remember-me])
        {:class "opux-input__label opux-input__label--checkbox"
         :for (name :pages.login.remember-me)})

       [:input
        {:class "opux-button"
         :type "submit"
         :value (tr [:actions.user.login])}]]]]))
