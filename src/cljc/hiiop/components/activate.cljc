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

(rum/defcs activate < (rum/local "" ::confirm-password)
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

    #?(:cljs
       (add-watch
        activation-info
        ::activation-info-validator
        (fn [key _ old new]
          (let [value-or-error (checker new)]
            (log/info value-or-error)
            (cond
              (:--value value-or-error) (set-valid! true)
              (:--error value-or-error) (set-valid! false))))))


    #?(:cljs
       (when (not @processed)
         (go
           (let [token-info (<! (api/validate-token {:token token}))]
             (reset! processed true)
             (reset! is-valid-token (nil? (:errors token-info)))
             (if @is-valid-token
               (do
                 (swap! activation-info #(assoc % :email (:email token-info)))
                 (swap! activation-info #(assoc % :token (:token token-info))))
               (reset! token-error (:errors token-info)))))))

    #?(:cljs
       (if @activated
         [:h1 "ACTIVATED"]

         (cond
           (nil? @is-valid-token) [:div {:class "loading"}]

           (= false @is-valid-token) [:h1 (:token @token-error)]

           @is-valid-token
           [:form
            {:class "opux-form"
             :on-submit
             (fn [e]
               (.preventDefault e)
               (when @is-valid
                 (go
                   (let [success (<! (api/activate-user @activation-info))]
                     (if success
                       (reset! activated true))))))}

            [:div {:class "opux-form-section"}
             [:h2 (tr [:pages.activate.title])]

             [:div {:class "opux-fieldset opux-form-section__fieldset"}
              [:div {:class "opux-fieldset__item"}
               (html/label
                (tr [:pages.activate.password])
                {:class "opux-input__label password-label"})
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
                {:class "opux-input__label password-label"})
               (html/input
                {:schema (get-in cursors-and-schema [:password :schema])
                 :value (get-in cursors-and-schema [:confirm-password :value])
                 :type "password"
                 :class "opux-input opux-input--text confirm-password"
                 :error (get-in cursors-and-schema [:confirm-password :error])})]

              [:input
               {:class "opux-button"
                :type "submit"
                :value (tr [:actions.user.save-password])}]]]]))

       :clj
       [:div {:class "empty"}])))
