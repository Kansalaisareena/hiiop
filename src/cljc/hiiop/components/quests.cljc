(ns hiiop.components.quests
  (:require [clojure.string :as str]
            [rum.core :as rum]
            [taoensso.timbre :as log]
            [schema.core :as s]
            #?(:cljs [cljs.core.async :refer [<!]])
            #?(:cljs [hiiop.client-api :as api])
            [hiiop.time :as time]
            [hiiop.components.core :as c]
            [hiiop.html :as html]
            [hiiop.schema :as hs]))

(rum/defc display [quest]
  [:li quest])

(rum/defc list-quests [{:keys [context quests]}]
  (let [tr (:tr context)]
    [:div
     [:h2 (tr [:pages.quest.title])]
     [:ul
      (map display quests)]]))

(defn add-organisation-to [quest]
  (swap! quest
         #(identity
           (assoc (deref quest)
                  :organisation
                  {:name ""
                   :description ""}))))

(defn add-to-errors [errors values]
  (swap! errors
         #(identity
           (conj (deref errors)
                 values))))

(defn enable-organisation [enable]
  (swap! enable
         #(identity true)))

(rum/defcs edit-content < rum/reactive
  (rum/local false ::organisation-enabled)
  [state {:keys [context quest schema errors is-valid cursors-and-schema tr]}]
  (let [organisation-enabled (::organisation-enabled state)]
    (html/form-section
     (tr [:pages.quest.edit.subtitles.content])
     [:div {:class "opux-fieldset__item"}
      (html/label
       (tr [:pages.quest.edit.name])
       {:class "opux-input__label"
        :error (get-in cursors-and-schema [:name :error])})
      (html/input
       {:class "opux-input opux-input--text"
        :type "text"
        :value (get-in cursors-and-schema [:name :value])
        :error (get-in cursors-and-schema [:name :error])
        :schema (get-in cursors-and-schema [:name :schema])
        :context context})]
     [:div {:class "opux-fieldset__item"}
      (html/label
       (tr [:pages.quest.edit.description])
       {:class "opux-input__label opux-input__label--unmoderated-description"
        :error (get-in cursors-and-schema [:unmoderated-description :error])})
      (html/text
       {:class "opux-input opux-input--textarea opux-input--textarea--unmoderated-description"
        :value (get-in cursors-and-schema [:unmoderated-description :value])
        :error (get-in cursors-and-schema [:unmoderated-description :error])
        :schema (get-in cursors-and-schema [:unmoderated-description :schema])
        :context context})]
     [:div {:class "opux-fieldset__item"}
      (html/label
       (tr [:pages.quest.edit.hashtags])
       {:class "opux-input__label opux-input__label--hashtags-label"
        :error (get-in cursors-and-schema [:hashtags :error])})
      (html/input
       {:class "opux-input opux-input--text opux-input--text--hashtags"
        :type "text"
        :value (get-in cursors-and-schema [:hashtags :value])
        :error (get-in cursors-and-schema [:hashtags :error])
        :schema (get-in cursors-and-schema [:hashtags :schema])
        :error-key :error.hashtag
        :transform-value #(if (string? %) (str/split % #" ") %)
        :to-value #(if (sequential? %) (str/join " " %) %)
        :context context})]
     (if (not (rum/react organisation-enabled))
       (html/button
        (tr [:pages.quest.edit.button.add-organisation])
        {:class "opux-button opux-button--organisation"
         :on-click
         (fn [e]
           (enable-organisation organisation-enabled)
           (add-organisation-to quest)
           (add-to-errors
            errors
            {:organisation
             {:name nil
              :description nil}}))})
       (let [organisation-name-error (rum/cursor-in errors [:organisation :name])
             organisation-description-error (rum/cursor-in errors [:organisation :description])]
         [:div {:class "opux-fieldset__item"}
          (html/label
           (tr [:pages.quest.edit.organisation.name])
           {:class "opux-input__label opux-input__label--organisation-name"})
          (html/input
           {:class "opux-input opux-input--text"
            :type "text"
            :value (rum/cursor-in quest [:organisation :name])
            :error organisation-name-error
            :schema hs/NonEmptyString
            :context context}
           )]
         [:div {:class "opux-fieldset__item"}
          (html/label
           (tr [:pages.quest.edit.organisation.description])
           {:class "opux-input__label opux-input__label--organisation-description"})
          (html/text
           {:class "opux-input opux-input--textarea testingshit"
            :value (rum/cursor-in quest [:organisation :description])
            :error organisation-description-error
            :schema hs/NonEmptyString
            :context context}
           )])))))

(defn reveal-end-time [end-time-revealed]
  (swap! end-time-revealed #(identity true)))

(rum/defcs edit-time-place < rum/reactive
  (rum/local false ::end-time-revealed)
  [state {:keys [quest is-valid cursors-and-schema context tr]}]
  (let [end-time-revealed (::end-time-revealed state)
        end-time (get-in cursors-and-schema [:end-time :value])
        start-time (get-in cursors-and-schema [:start-time :value])]
    (add-watch
     start-time
     ::use-same-date-when-not-revealed
     (fn [_ _ _ new-start]
       (let [start-time-o (time/from-string new-start)
             end-time-o (time/from-string @end-time)]
         (when (and (not @end-time-revealed)
                    (time/after?
                     start-time-o
                     end-time-o))
           (swap! end-time #(time/to-string
                             (time/use-same-date
                              start-time-o
                              end-time-o))))
         )))
    (html/form-section
     (tr [:pages.quest.edit.subtitles.time-place])
     [:div {:class "opux-fieldset__item"}
      (html/label
       (tr [:pages.quest.edit.start-time])
       {:class "opux-input__label start-time-label"
        :error (atom nil)})
      (html/datetime-picker
       {:date start-time
        :error (get-in cursors-and-schema [:start-time :error])
        :schema (get-in cursors-and-schema [:start-time :schema])
        :max-date (when (rum/react end-time-revealed)
                    end-time)
        :class "opux-fieldset__item opux-fieldset__item--inline-container start-time"
        :value-format time/transit-format
        :date-print-format time/date-print-format
        :time-print-format time/time-print-format
        :context context})]

     (if (not (rum/react end-time-revealed))
       [:div {:class "opux-fieldset__item"}
        (html/button
         (tr [:pages.quest.edit.button.reveal-end-time])
         {:class "opux-button end-time-reveal"
          :on-click (fn [e]
                      (reveal-end-time end-time-revealed))})]
       [:div {:class "opux-fieldset__item"}
        (html/label
         (tr [:pages.quest.edit.end-time])
         {:class "opux-input__label end-time-label"
          :error (get-in cursors-and-schema [:end-time :error])})
        (html/datetime-picker
         {:date end-time
          :error (get-in cursors-and-schema [:end-time :error])
          :schema (get-in cursors-and-schema [:end-time :schema])
          :min-date (get-in cursors-and-schema [:start-time :value])
          :class "opux-fieldset__item opux-fieldset__item--inline-container end-time"
          :value-format time/transit-format
          :date-print-format time/date-print-format
          :time-print-format time/time-print-format
          :context context})])
     (html/label
      (tr [:pages.quest.edit.location.label])
      {:class "location-label"
       :error (get-in cursors-and-schema [:location :error])}
      (html/location-selector
       {:location (get-in cursors-and-schema [:location :value])
        :error (get-in cursors-and-schema [:location :error])
        :schema (get-in cursors-and-schema [:location :schema])
        :placeholder (tr [:pages.quest.edit.location.placeholder])
        :context context}))
     )))

(rum/defc edit-participation-settings < rum/reactive
  [{:keys [quest is-valid cursors-and-schema context tr]}]
  (html/form-section
   (tr [:pages.quest.edit.subtitles.related-to])
   (html/multi-selector-for-schema
    {:schema (get-in cursors-and-schema [:categories :schema])
     :value (get-in cursors-and-schema [:categories :value])
     :error (get-in cursors-and-schema [:categories :error])
     :context context})
   )
  (html/form-section
   (tr [:pages.quest.edit.subtitles.participation])
   (html/max-participants
    {:schema (get-in cursors-and-schema [:max-participants :schema])
     :value (get-in cursors-and-schema [:max-participants :value])
     :error (get-in cursors-and-schema [:max-participants :error])
     :context context})
   (html/radio-binary
    {:class "is-open"
     :schema (get-in cursors-and-schema [:is-open :schema])
     :value (get-in cursors-and-schema [:is-open :value])
     :error (get-in cursors-and-schema [:is-open :error])
     :context context}
    {:pages.quest.edit.open true
     :pages.quest.edit.closed false})
   (html/checkbox-binary
    {:class "organiser-participates"
     :id (name :pages.quest.edit.organiser-participates)
     :schema (get-in cursors-and-schema [:organiser-participates :schema])
     :value (get-in cursors-and-schema [:organiser-participates :value])
     :error (get-in cursors-and-schema [:organiser-participates :error])
     })
   (html/label
    (tr [:pages.quest.edit.organiser-participates])
    {:for (name :pages.quest.edit.organiser-participates)})
   ))

(rum/defc edit < rum/reactive
  [{:keys [context quest schema errors]}]
  (let [tr (:tr context)
        cursors-and-schema (c/value-and-error-cursors-and-schema {:for quest
                                                                  :schema schema
                                                                  :errors errors})
        is-valid (atom false)
        checker (partial hs/select-schema-either schema)
        set-valid! (fn [validity]
                     (swap! is-valid #(identity validity)))
        show-end-time (atom false)]
    (add-watch
     quest
     ::quest-validator
     (fn [key _ old new]
       (let [value-or-error (checker new)]
         (log/info value-or-error)
         (cond
           (:--value value-or-error) (set-valid! true)
           (:--error value-or-error) (set-valid! false)))))
    [:form
     {:class "opux-form"
      :on-submit (fn [e] (.preventDefault e))}
     [:h1 (tr [:actions.quest.create])]
     (edit-content
      {:cursors-and-schema cursors-and-schema
       :is-valid is-valid
       :context context
       :quest quest
       :errors errors
       :tr tr
       })
     (edit-time-place
      {:cursors-and-schema cursors-and-schema
       :is-valid is-valid
       :context context
       :quest quest
       :errors errors
       :tr tr
       })
     (html/form-section
      (tr [:pages.quest.edit.subtitles.related-to])
      (html/multi-selector-for-schema
       {:schema (get-in cursors-and-schema [:categories :schema])
        :value (get-in cursors-and-schema [:categories :value])
        :error (get-in cursors-and-schema [:categories :error])
        :context context})
      )
     (edit-participation-settings
      {:cursors-and-schema cursors-and-schema
       :is-valid is-valid
       :context context
       :quest quest
       :tr tr
       })
     (html/form-section
      (html/button
       (tr [:pages.quest.edit.button.submit])
       {:class "opux-button opux-form__button"
        :active is-valid})
      (html/button
       (tr [:pages.quest.edit.button.remove])
       {:class "opux-button opux-form__button"}))
     ]))
