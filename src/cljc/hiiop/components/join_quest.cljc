(ns hiiop.components.join-quest
  #?(:cljs
     (:require-macros [cljs.core.async.macros :refer [go]])
     )
  (:require [clojure.string :as string]
            #?(:cljs [cljs.core.async :refer [<!]])
            [taoensso.timbre :as log]
            [schema.core :as s]
            [schema.coerce :as sc]
            [rum.core :as rum]
            [bidi.bidi :refer [path-for]]
            [hiiop.routes.page-hierarchy :refer [hierarchy]]
            [hiiop.schema :as hs]
            [hiiop.components.core :as c]
            [hiiop.mangling :as mangling]
            [hiiop.html :as html]
            #?(:cljs [hiiop.client-api :as api])))

(rum/defc signup-form < rum/reactive
  [{:keys [context value schema error is-valid]}]
  (let [tr (:tr context)
        errors (atom (mangling/same-keys-with-nils @value))
        cursors-and-schema (c/value-and-error-cursors-and-schema
                            {:for value
                             :schema schema
                             :errors errors})
        checker (partial hs/select-schema-either schema)
        set-valid! (fn [validity] (reset! is-valid validity))]

    (add-watch
     value
     ::quest-signup-validator
     (fn [key _ old new]
       (let [value-or-error (checker new)]
         (cond
           (:--value value-or-error)
           (do
             (set-valid! true)
             (reset! error nil))
           (:--error value-or-error)
           (do
             (set-valid! false)
             (reset! error true))))))

    [:div {:class "opux-fieldset__item"}
     (html/label
      (tr [:pages.quest.view.signup.name])
      {:class "opux-input__label"
       :error (get-in cursors-and-schema [:name :error])})
     (html/input
      {:class "opux-input opux-input--text"
       :type "text"
       :error (get-in cursors-and-schema [:name :error])
       :value (get-in cursors-and-schema [:name :value])
       :schema (get-in cursors-and-schema [:name :schema])
       :context context})

     (html/label
      (tr [:pages.quest.view.signup.email])
      {:class "opux-input__label"
       :error (get-in cursors-and-schema [:email :error])})
     (html/input
      {:class "opux-input opux-input--text"
       :type "text"
       :error (get-in cursors-and-schema [:email :error])
       :value (get-in cursors-and-schema [:email :value])
       :schema (get-in cursors-and-schema [:email :schema])
       :context context})

     (html/label
      (tr [:pages.quest.view.signup.phone])
      {:class "opux-input__label"
       :error (get-in cursors-and-schema [:phone :error])})
     (html/input
      {:class "opux-input opux-input--text"
       :type "text"
       :error (get-in cursors-and-schema [:phone :error])
       :value (get-in cursors-and-schema [:phone :value])
       :schema (get-in cursors-and-schema [:phone :schema])
       :context context})
     [:div {:class "opux-fieldset__item"}
      (html/checkbox-binary
       {:class "opux-checkbox-binary opux-fieldset__item"
        :id (name :pages.quest.view.signup.agreement)
        :schema (get-in cursors-and-schema [:agreement :schema])
        :error (get-in cursors-and-schema [:agreement :error])
        :value (get-in cursors-and-schema [:agreement :value])
        })
      (html/label
       (tr [:pages.quest.view.signup.agree-to-terms-and-conditions])
       {:class "opux-input__label opux-input__label--checkbox"
        :for (name :pages.quest.view.signup.agreement)
        :error (get-in cursors-and-schema [:agreement :error])})
      ]]
    ))

(rum/defc join-quest-form
  [{:keys [context party-member schema errors quest-id view-state is-valid signup-valid days-between]}]
  (let [tr (:tr context)
        cursors-and-schema
        (c/value-and-error-cursors-and-schema {:for party-member
                                               :schema schema
                                               :errors errors})
        ]
    [:form
     {:class "opux-form opux-content opux-content--small"
      :on-submit
      (fn [e]
        (.preventDefault e)
        #?(:cljs
           (go
             (let [to-api (if (:user-id @party-member)
                            (assoc @party-member :user-id
                                   (str (:user-id @party-member)))
                            @party-member)
                   from-api (<! (api/join-quest quest-id to-api))]
               (if (:success from-api)
                 (reset! view-state {:view "success"
                                     :message (tr [:pages.quest.view.join.success])})
                 (reset! view-state {:view "fail"
                                     :message (tr [:pages.quest.view.join.fail])})
               ))))
        )
      }
     [:div {:class "opux-centered"}
      (html/wrap-paragraph (tr [:pages.quest.view.signup.subtitle]))]

     (when (not (get-in context [:identity :id]))
       (signup-form
        {:context context
         :value (get-in cursors-and-schema [:signup :value])
         :schema hs/QuestSignup
         :error (get-in cursors-and-schema [:signup :error])
         :is-valid signup-valid}))

     (if (> days-between 1)
       [:div {:class "opux-fieldset__item opux-centered"}
        (html/label
          (tr [:pages.quest.view.signup.days])
          {:class "opux-input__label"
           :error (get-in cursors-and-schema [:days :error])})
        (html/number-input-with-ticker
          {:class "opux-input opux-input--text opux-input--text--centered centered"
           :error (get-in cursors-and-schema [:days :error])
           :value (get-in cursors-and-schema [:days :value])
           :type "number"
           :schema hs/NPlus
           :transform-value #(if (string? %) (mangling/parse-natural-number %))
           :context context})])

     [:div {:class "opux-fieldset__item opux-centered"}
      (html/button
       (tr [:actions.quest.signup])
       {:class "opux-button opux-button--highlight opux-form__button"
        :type "submit"
        :active is-valid})]]))

(rum/defc show-message [{:keys [context message secret-party quest-id]}]
  (let [quest-link (path-for hierarchy :quest :quest-id quest-id)]
  [:div {:class "opux-content opux-centered"}
   [:p message]
   [:a {:class "opux-button" :href quest-link}
    ((:tr context) [:pages.quest.view.join.to-front-page])]]))

(rum/defcs join-quest < rum/reactive
                        (rum/local false ::is-valid)
                        (rum/local false ::signup-valid)
                        (rum/local {:view "join"} ::view)
  [state {:keys [quest-id context party-member schema errors secret-party days-between]}]
  (let [tr (:tr context)
        view-state (::view state)
        view (:view @view-state)
        message (:message @view-state)
        use-signup (nil? (get-in context [:identity :id]))
        signup-valid (::signup-valid state)
        checker (partial hs/select-schema-either schema)
        is-valid (::is-valid state)
        set-valid! (fn [validity] (reset! is-valid validity))
        check-and-set-valid!
        (fn [new]
          (let [value-or-error (checker new)]
            (cond

              (and (:--value value-or-error) (not use-signup))
              (do
                (set-valid! true))

              (and (:--value value-or-error) use-signup @signup-valid)
              (do
                (set-valid! true))

              (:--error value-or-error) (set-valid! false)
              :else (set-valid! false))))]

    (when secret-party
      (do
        (-> @party-member
            (assoc :secret-party secret-party)
            (#(reset! party-member %1))
            (check-and-set-valid!))))

    (when (not use-signup)
      (do
        (-> @party-member
            (assoc :user-id (sc/string->uuid
                             (get-in context [:identity :id])))
            (dissoc :signup)
            (#(reset! party-member %1))
            (check-and-set-valid!))))

    (add-watch
     party-member
     ::quest-signup-form-validator
     (fn [key _ old new]
       (check-and-set-valid! new)))

    (cond
      (= view "join")
      (join-quest-form
       {:context context
        :party-member party-member
        :quest-id quest-id
        :days-between days-between
        :view-state view-state
        :schema schema
        :errors errors
        :is-valid is-valid
        :signup-valid signup-valid})

      (= view "success")
      (show-message {:message message
                     :context context
                     :quest-id quest-id})

      (= view "fail")
      (show-message {:message message
                     :context context
                     :quest-id quest-id})
      )))
