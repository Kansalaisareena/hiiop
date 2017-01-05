(ns hiiop.components.activate
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]
                            [clojure.string :as cs]
                            [hiiop.schema :as hs]))
  (:require
   #?(:cljs [cljs.core.async :refer [<!]])
   #?(:cljs [hiiop.client-api :as api])
   [rum.core :as rum]
   [schema.core :as s]
   [schema.coerce :as sc]
   [hiiop.components.core :as c]
   [hiiop.schema :as hs]
   [taoensso.timbre :as log]
   [bidi.bidi :refer [path-for]]
   [hiiop.html :as html]
   [hiiop.routes.page-hierarchy :as pages]))

(rum/defc password-form < rum/reactive
  [{:keys [cursors-and-schema
           activated
           is-valid
           activation-info
           tr
           context]}]
  [:form
   {:class "opux-form"
    :on-submit
    (fn [e]
      (.preventDefault e)
      #?(:cljs
         (when @is-valid
           (go
             (let [success (<! (api/activate-user @activation-info))]
               (if success
                 (reset! activated true)))))))}

   [:div {:class "opux-form-section"}
    [:h2 (tr [:pages.activate.title])]

    [:div {:class "opux-fieldset opux-form-section__fieldset"}
     [:div {:class "opux-fieldset__item"}
      (html/label
       (tr [:pages.activate.password])
       {:class "opux-input__label password-label"
        :required true})
      (html/input
       {:schema (get-in cursors-and-schema [:password :schema])
        :value (get-in cursors-and-schema [:password :value])
        :type "password"
        :context context
        :class "opux-input opux-input--text password"
        :error (get-in cursors-and-schema [:password :error])})]

     [:div {:class "opux-fieldset__item"}
      (html/label
       (tr [:pages.activate.confirm-password])
       {:class "opux-input__label password-label"
        :required true})
      (html/input
       {:schema (get-in cursors-and-schema [:password :schema])
        :value (get-in cursors-and-schema [:confirm-password :value])
        :type "password"
        :class "opux-input opux-input--text confirm-password"
        :error (get-in cursors-and-schema [:confirm-password :error])})]

     (html/button
      (tr [:actions.user.save-password])
      {:type "submit"
       :class "opux-button"
       :active is-valid})]]])

(rum/defcs activate < rum/reactive
                      (rum/local "" ::confirm-password)
                      (rum/local false ::processed)
                      (rum/local nil ::token-error)
                      (rum/local nil ::is-valid-token)
                      (rum/local false ::activated)
  [state {:keys [context token activation-info schema errors]}]
  (let [is-valid-token (::is-valid-token state)
        confirm-password (::confirm-password state)
        processed (::processed state)
        token-error (::token-error state)
        activated (::activated state)
        tr (:tr context)
        cursors-and-schema
        (c/value-and-error-cursors-and-schema {:for activation-info
                                               :schema schema
                                               :errors errors})
        checker (partial hs/select-schema-either schema)
        is-valid (atom false)
        set-valid! (fn [validity] (reset! is-valid validity))]

    (add-watch
     activation-info
     ::activation-info-validator
     (fn [key _ old new]
       (let [value-or-error (checker new)]
         (log/info value-or-error)
         (cond
           (:--value value-or-error) (set-valid! true)
           (:--error value-or-error) (set-valid! false)))))

    (when (not (rum/react processed))
      #?(:cljs
         (go
           (let [token-info (<! (api/validate-token {:token token}))
                 error (get-in token-info [:errors :token])
                 error-key (keyword error)]
             (reset! processed true)
             (reset! is-valid-token (nil? (:errors token-info)))
             (if @is-valid-token
               (do
                 (swap! activation-info #(assoc % :email (:email token-info)))
                 (swap! activation-info #(assoc % :token (:token token-info))))
               (reset! token-error (tr [error-key])))))))

    (if (rum/react activated)
      [:div
       [:h1 (tr [:pages.activate.done.title])]
       [:p
        [:a
         {:href (path-for pages/hierarchy :login)}
         (tr [:pages.activate.done.text])]]]
      (cond
        (nil? (rum/react is-valid-token)) [:div {:class "loading"}]

        (= false (rum/react is-valid-token)) [:h1 @token-error]

        (rum/react is-valid-token)
        (password-form
         {:is-valid is-valid
          :cursors-and-schema cursors-and-schema
          :activated activated
          :activation-info activation-info
          :tr tr
          :context context})))
    ))
