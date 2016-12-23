(ns hiiop.components.quest-signup-form
  (:require [clojure.string :as string]
            [hiiop.schema :as hs]
            [hiiop.components.core :as c]
            [hiiop.mangling :as mangling]
            [hiiop.schema :as hs]
            [taoensso.timbre :as log]
            [schema.core :as s]
            [rum.core :as rum]
            [hiiop.html :as html]
            [clojure.string :as str]))

(rum/defc signup-form < rum/reactive
  [{:keys [context quest-signup-info schema errors]}]
  (let [tr (:tr context)
        cursors-and-schema
        (c/value-and-error-cursors-and-schema {:for quest-signup-info
                                               :schema schema
                                               :errors errors})
        checker (partial hs/select-schema-either schema)
        is-valid (atom false)
        set-valid! (fn [validity] (reset! is-valid validity))]

    (add-watch
     quest-signup-info
     ::quest-signup-info-validator
     (fn [key _ old new]
       (let [value-or-error (checker new)]
         (cond
           (:--value value-or-error) (set-valid! true)
           (:--error value-or-error) (set-valid! false)))))

    [:form {:class "opux-form opux-content opux-content--small"}

     [:div {:class "opux-centered"}
      (html/wrap-paragraph (tr [:pages.quest.single.signup-form-subtitle]))]

      [:div {:class "opux-fieldset__item"}
       (html/label
        (tr [:pages.quest.single.participants-name])
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
        (tr [:pages.quest.single.participants-email])
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
        (tr [:pages.quest.single.participants-phone])
        {:class "opux-input__label"
         :error (get-in cursors-and-schema [:phone :error])})
       (html/input
        {:class "opux-input opux-input--text"
         :type "text"
         :error (get-in cursors-and-schema [:phone :error])
         :value (get-in cursors-and-schema [:phone :value])
         :schema (get-in cursors-and-schema [:phone :schema])
         :context context})]

      [:div {:class "opux-fieldset__item"}
       (html/label
        (tr [:pages.quest.single.number-of-participating-days])
        {:class "opux-input__label"
         :error (get-in cursors-and-schema [:participate-days :error])})
        (html/number-input-with-ticker
         {:class "opux-input opux-input--text"
          :error (get-in cursors-and-schema [:participate-days :error])
          :value (get-in cursors-and-schema [:participate-days :value])
          :type "number"
          :schema (get-in cursors-and-schema [:participate-days :schema])
          :transform-value #(if (string? %) (mangling/parse-natural-number %))
          :context context})]

      [:div {:class "opux-fieldset__item"}
       (html/checkbox-binary
        {:class "organiser-participates opux-fieldset__item"
         :id (name :pages.quest.single.agreement)
         :schema (get-in cursors-and-schema [:agreement :schema])
         :error (get-in cursors-and-schema [:agreement :error])
         :value (get-in cursors-and-schema [:agreement :value])})
       (html/label
        (tr [:pages.quest.single.agree-to-terms-and-conditions])
        {:class "opux-input__label opux-input__label--checkbox"
         :for (name :pages.quest.single.agreement)
         :error (get-in cursors-and-schema [:agreement :error])})]

       [:div {:class "opux-fieldset__item opux-fieldset__item--inline-container"}
        (html/button
         (tr [:actions.quest.cancel])
         {:class "opux-button opux-button--dull opux-form__button opux-fieldset__inline-item"
          :type "submit"})
        (html/button
         (tr [:actions.quest.signup])
         {:class "opux-button opux-button--highlight opux-form__button opux-fieldset__inline-item"
          :active is-valid})]]))
