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
     [:h2 (tr [:page.register.check-your-email])]
     [:p (tr [:page.register.email-sent-message])]]))

(rum/defcs register < (rum/local false ::registered)
  [state {:keys [context registration-info schema errors]}]
  (let [registered (::registered state)
        tr (:tr context)
        cursors-and-schema
        (c/value-and-error-cursors-and-schema {:for registration-info
                                               :schema schema
                                               :errors errors})
        checker (partial hs/select-schema-either schema)
        is-valid (atom false)
        set-valid! (fn [validity] (reset! is-valid validity))]

    (add-watch
     registration-info
     ::registration-info-validator
     (fn [key _ old new]
       (let [value-or-error (checker new)]
         (log/info value-or-error)
         (cond
           (:--value value-or-error) (set-valid! true)
           (:--error value-or-error) (set-valid! false)))))

    (if @registered
      (registered-notice context)
      [:form
       {:class "opux-form"
        :on-submit
        (fn [e]
          (.preventDefault e)
          (when @is-valid
            #?(:cljs
               (go
                 (let [success (<! (api/register @registration-info))]
                   (if success
                     (swap! registered not)))))))}

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
            :error (get-in cursors-and-schema [:email :error])})]

         [:input
          {:class "opux-button"
           :type "submit"
           :value (tr [:actions.user.register])}]]]])))
