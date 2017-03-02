(ns hiiop.components.password-reset
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require
   [rum.core :as rum]
   [schema.core :as s]
   [taoensso.timbre :as log]
   #?(:cljs [cljs.core.async :refer [<!]])
   #?(:cljs [hiiop.client-api :as api])
   [hiiop.schema :as schema]
   [hiiop.html :as html]
   [hiiop.url :as u]
   [hiiop.routes.page-hierarchy :as pages]))

(defn get-error [from]
  (log/info from)
  :errors.email.not-valid)

(rum/defc display-message < rum/reactive
  [{:keys [context title-key message-key link-to]}]
  (log/info "display message" title-key message-key link-to)
  (let [tr (:tr context)]
    [:div {:class "opux-content opux-centered"}
     [:h2 (tr [title-key])]
     [:p {:class "opux-section"}
      (if link-to
        [:a {:href link-to} (tr [message-key])]
        (tr [message-key]))]]))

(rum/defcs password-reset-form < rum/reactive
                                 (rum/local {:password ""
                                             :confirm-password ""}
                                            ::passwords)
                                 (rum/local nil ::error)
  [state {:keys [view token api-fn context]}]
  (log/info "password-reset-form" (:password @(::passwords state)) (:confirm-password @(::passwords state)))
  (let [tr (:tr context)
        type (::type state)
        passwords (::passwords state)
        password (rum/cursor-in passwords [:password])
        confirm-password (rum/cursor-in passwords [:confirm-password])
        check-password (schema.core/checker schema/Password)
        are-same (= @password @confirm-password)
        error-state (::error state)
        error (cond
                (and (not (nil? (check-password @password)))
                     (not (nil? (check-password @confirm-password))))
                :errors.password.not-valid

                (not are-same)
                :errors.password.not-same

                (or @error-state nil)
                (or @error-state nil))
        is-valid (atom
                  (and (= nil (check-password @password))
                       (= nil (check-password @confirm-password))
                       are-same))]

    [:form
     {:class "opux-form"
      :on-submit
      (fn [e]
        (.preventDefault e)
        #?(:cljs
           (go
             (reset! error-state nil)
             (let [response (<! (api-fn {:token (str token)
                                         :password @password}))]
               (if (:success response)
                 (reset! view "success")
                 (reset! error-state :errors.token.expired))))
           ))}
     [:div {:class "opux-form-section"}
      [:h2 (tr [:pages.activate.title])]

      [:div {:class "opux-fieldset opux-form-section__fieldset"}

       [:div {:class "opux-fieldset__item"}
        (html/label
         (tr [:pages.login.password])
         {:class "opux-input__label user-label"
          :required true
          :for (name :pages.password-reset.password)})
        (html/input
         {:schema s/Str
          :value password
          :type "password"
          :id (name :pages.password-reset.password)
          :class "opux-input opux-input--text password"
          :error (atom nil)
          :context context})
        (html/input
         {:schema s/Str
          :value password
          :type "text"
          :class "opux-input opux-input--hidden"
          :error (atom nil)
          :context context})
        (html/label
         (tr [:pages.activate.confirm-password])
         {:class "opux-input__label user-label"
          :required true
          :for (name :pages.password-reset.confirm-password)})
        (html/input
         {:schema s/Str
          :value confirm-password
          :type "password"
          :id (name :pages.password-reset.confirm-password)
          :class "opux-input opux-input--text password"
          :error (atom error)
          :context context})
        (html/input
         {:schema s/Str
          :value confirm-password
          :type "text"
          :class "opux-input opux-input--hidden"
          :error (atom nil)
          :context context})
        (when
          [:span {:class "opux-input__label--error"}
           (tr [:errors.password.not-same])])
        (when
          [:span {:class "opux-input__label--error"} (tr [:errors.password.not-valid])])
        ]

       [:div {:class "opux-centered"}
        (html/button
         (tr [:actions.user.save-password])
         {:class "opux-button opux-button--highlight"
          :type "submit"
          :active is-valid})
        ]]]]))

(rum/defcs password-reset < rum/reactive
                            (rum/local "form" ::view)
  [state {:keys [token
                 api-fn
                 context
                 done-title
                 done-text
                 done-link]}]
  (let [view (::view state)]
    (cond
      (= @view "form") (password-reset-form
                        {:context context
                         :token token
                         :view view
                         :api-fn api-fn
                         :message (::message state)})

      (= @view "success") (display-message
                           {:context context
                            :title-key done-title
                            :message-key done-text
                            :link-to done-link})
      )))

(rum/defcs request-password-reset-form < rum/reactive
                                         (rum/local "" ::email)
                                         (rum/local nil ::error)
  [state {:keys [view context]}]
  (let [tr (:tr context)
        email (::email state)
        error (::error state)]
    [:form
     {:class "opux-form"
      :on-submit
      (fn [e]
        (.preventDefault e)
        #?(:cljs
           (go
             (reset! error nil)
             (let [response (<! (api/request-reset-password @email))]
               (if (:success response)
                 (reset! view "success")
                 (reset! error (get-error (:body response))))))
           ))}
     [:div {:class "opux-form-section"}
      [:h2 (tr [:pages.password-reset.title])]

      [:div {:class "opux-fieldset opux-form-section__fieldset"}

       [:p {:class "opux-content"} (tr [:pages.password-reset.text])]

       [:div {:class "opux-fieldset__item"}
        (html/label
         (tr [:pages.login.email])
         {:class "opux-input__label user-label"
          :required true})
        (html/input
         {:schema schema/Email
          :value email
          :type "text"
          :class "opux-input opux-input--text email"
          :error error
          :context context})]

       [:div {:class "opux-centered"}
        [:button
         {:class "opux-button opux-button--highlight"
          :type "submit"}
         (tr [:pages.password-reset.send])]]]]]))

(rum/defcs request-password-reset < rum/reactive
                                    (rum/local "form" ::view)
                                    (rum/local {:title
                                                :pages.password-reset.check-email.title
                                                :text
                                                :pages.password-reset.check-email.text}
                                               ::message)
  [state {:keys [context]}]
  (let [view (::view state)]
    (cond
      (= @view "form") (request-password-reset-form
                        {:context context
                         :view view
                         :message (::message state)})

      (= @view "success") (display-message
                           {:context context
                            :title-key (:title @(::message state))
                            :message-key (:text @(::message state))})
      )))
