(ns hiiop.components.register
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]
                            [hiiop.schema :as hs]))
  (:require
   [rum.core :as rum]
   [schema.core :as s]
   [hiiop.schema :as schema]
   [hiiop.components.core :as c]
   [taoensso.timbre :as log]
   [bidi.bidi :refer [path-for]]
   #?(:cljs [cljs.core.async :refer [<!]])
   #?(:cljs [hiiop.client-api :as api])
   [hiiop.html :as html]
   [hiiop.schema :as hs]
   [hiiop.routes.page-hierarchy :as pages]))

(rum/defc registered-notice [context]
  (let [tr (:tr context)]
    [:div {:class "opux-content opux-content--small opux-centered"}
     [:h2 (tr [:pages.register.check-your-email])]
     [:p (tr [:pages.register.email-sent])]]))

(rum/defcs registration-form < rum/reactive
                             < (rum/local false ::is-valid)
  [state
   {:keys [cursors-and-schema
           registration-info
           registered
           is-valid
           context
           tr]}]
  (let [local-is-valid (::is-valid state)
        email-error (get-in cursors-and-schema [:email :error])]
    (add-watch is-valid ::see-if-valid
               (fn [_ _ _ new]
                 (reset! local-is-valid new)))
    [:form
     {:class "opux-form"
      :on-submit
      (fn [e]
        (.preventDefault e)
        (when @local-is-valid
          #?(:cljs
             (go
               (let [response (<! (api/register @registration-info))
                     success (:success response)
                     error (get-in response [:errors :email])]
                 (if (:success response)
                   (reset! registered true)
                   (reset! email-error (tr [(keyword error)]))))))))}

     [:div {:class "opux-form-section"}
      [:h2 (tr [:pages.register.title])]
      [:div {:class "opux-fieldset opux-form-section__fieldset"}

       [:div {:class "opux-fieldset__item"}
        (html/label
         (tr [:pages.register.name])
         {:class "opux-input__label name-label"})
        (html/input
         {:schema (get-in cursors-and-schema [:name :schema])
          :value (get-in cursors-and-schema [:name :value])
          :type "text"
          :class "opux-input opux-input--text name"
          :context context
          :error (get-in cursors-and-schema [:name :error])})]

       [:div {:class "opux-fieldset__item"}
        (html/label
         (tr [:pages.register.email])
         {:class "opux-input__label name-label"})
        (html/input
         {:schema (get-in cursors-and-schema [:email :schema])
          :value (get-in cursors-and-schema [:email :value])
          :type "text"
          :context context
          :class "opux-input opux-input--text email"
          :error (get-in cursors-and-schema [:email :error])
          :error-key :errors.email.not-valid})]

       (html/button
        (tr [:actions.user.register])
        {:class "opux-button"
         :type "submit"
         :active local-is-valid}
        )]]]))

(rum/defcs register < rum/reactive
                      (rum/local false ::registered)
  [state {:keys [context registration-info schema errors]}]
  (let [registered (::registered state)
        tr (:tr context)
        cursors-and-schema (c/value-and-error-cursors-and-schema
                            {:for registration-info
                             :schema schema
                             :errors errors})
        checker (partial hs/select-schema-either schema)
        is-valid (atom false)
        set-valid! (fn [validity] (reset! is-valid validity))]

    (add-watch
     registration-info
     ::registration-info-validator
     (fn [_ _ _ new]
       (let [value-or-error (checker new)]
         (log/info value-or-error)
         (cond
           (:--value value-or-error) (set-valid! true)
           (:--error value-or-error) (set-valid! false)))))

    (if (rum/react registered)
      (registered-notice context)
      (registration-form
       {:registration-info registration-info
        :cursors-and-schema cursors-and-schema
        :is-valid is-valid
        :context context
        :registered registered
        :tr tr})
      )))
