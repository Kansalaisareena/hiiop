(ns hiiop.components.quests
  #?(:cljs
     (:require-macros [cljs.core.async.macros :refer [go]])
     )
  (:require [clojure.string :as str]
            [rum.core :as rum]
            [taoensso.timbre :as log]
            [schema.core :as s]
            [schema.coerce :as sc]
            #?(:cljs [cljs.core.async :refer [<!]])
            #?(:cljs [hiiop.client-api :as api])
            [hiiop.time :as time]
            [hiiop.components.core :as c]
            [hiiop.html :as html]
            [hiiop.schema :as hs]))

(rum/defc card []
  [:div {:class "opux-card-container"}
   [:div {:class "opux-card"}

    [:div {:class "opux-card__image-container"}
     [:a {:href "#"}
      [:img {:class "opux-card__image"
             :src "https://media.giphy.com/media/HNQVf0ik57nHy/giphy-facebook_s.jpg"}]]]

    [:div {:class "opux-card__content"}
     [:span {:class "opux-card__location opux-inline-icon opux-inline-icon-location"}
      "Helsinki"]
     [:span {:class "opux-card__attendance opux-inline-icon opux-inline-icon-personnel opux-inline-icon--right"}
      23]

     [:a {:class "opux-card__title" :href "#"}
      "Konalan kehitysvammaisten iltatanhutapahtuma"]

     [:span {:class "opux-card__date opux-inline-icon opux-inline-icon-calendar"}
      "Keskiviikko 28.1"]
     [:span {:class "opux-card__time opux-inline-icon opux-inline-icon-clock"}
      "18.00-20.00"]]]])

(rum/defc display [quest]
  [:li quest])

(defn add-organisation-to [quest]
  (reset! quest
         (assoc (deref quest)
                :organisation
                {:name ""
                 :description ""})))

(defn add-to-errors [errors values]
  (reset! errors
          (conj (deref errors)
                 values)))

(defn enable-organisation [enable]
  (reset! enable true))

(rum/defcs picture-upload < rum/reactive
                            (rum/local nil ::picture-url)
  [state {:keys [cursors-and-schema context tr]}]
  (let [local-picture-url (::picture-url state)
        default-content [:div {:class "opux-fieldset__item"}]
        picture-url (get-in cursors-and-schema [:picture-url :value])]
    (when picture-url
      (do
        (reset! local-picture-url @picture-url)
        (add-watch
         picture-url
         ::local-picture-url
         (fn [key _ _ new]
           (reset! local-picture-url new)))))
    (into
     default-content
     (if (not (rum/react local-picture-url))
       [(html/label
         (tr [:pages.quest.edit.picture])
         {:class "opux-input__label opux-input__label--picture-label"
          :error (get-in cursors-and-schema [:picture-id :error])})
        (html/file-input
         {:value (get-in cursors-and-schema [:picture-id :value])
          :error (get-in cursors-and-schema [:picture-id :error])
          :context context
          :tr (partial tr [:page.quest.edit.picture.upload-failed])})
        ]
       [[:img {:src @picture-url
               :on-click
               (fn []
                 #?(:cljs
                    (do
                      (if (js/confirm (tr [:page.quest.edit.picture.remove]))
                        (reset! picture-url nil)
                      ))))
               }]])
     )
    ))

(rum/defcs edit-content < rum/reactive
  (rum/local false ::organisation-enabled)
  [state {:keys [context
                 quest
                 schema
                 errors
                 is-valid
                 cursors-and-schema
                 tr] :as args}]
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
       {:class "opux-input__label opux-input__label--description"
        :error (get-in cursors-and-schema [:description :error])})
      (html/text
       {:class "opux-input opux-input--textarea opux-input--textarea--unmoderated-description"
        :value (get-in cursors-and-schema [:description :value])
        :error (get-in cursors-and-schema [:description :error])
        :schema (get-in cursors-and-schema [:description :schema])
        :context context})]
     (picture-upload args)
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
        :context context})
      ]
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
  [state {:keys [cursors-and-schema context tr]}]
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
           (reset!
            end-time
            (time/to-string
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
     [:div {:class "opux-fieldset__item"}
      (html/label
       (tr [:pages.quest.edit.location.label])
       {:class "opux-input__label location-label"
        :error (get-in cursors-and-schema [:location :error])})
      (html/location-selector
       {:class "opux-input opux-input--location-selector"
        :location (get-in cursors-and-schema [:location :value])
        :error (get-in cursors-and-schema [:location :error])
        :schema (get-in cursors-and-schema [:location :schema])
        :placeholder (tr [:pages.quest.edit.location.placeholder])
        :context context})]
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
    {:class "is-open opux-fieldset__item"
     :schema (get-in cursors-and-schema [:is-open :schema])
     :value (get-in cursors-and-schema [:is-open :value])
     :error (get-in cursors-and-schema [:is-open :error])
     :context context}
    {:pages.quest.edit.open true
     :pages.quest.edit.closed false})
   (when (get-in cursors-and-schema [:organiser-participates :value])
     (html/checkbox-binary
      {:class "organiser-participates opux-fieldset__item"
       :id (name :pages.quest.edit.organiser-participates)
       :schema (get-in cursors-and-schema [:organiser-participates :schema])
       :value (get-in cursors-and-schema [:organiser-participates :value])
       :error (get-in cursors-and-schema [:organiser-participates :error])
       })
     (html/label
      (tr [:pages.quest.edit.organiser-participates])
      {:class "opux-input__label opux-input__label--checkbox"
       :for (name :pages.quest.edit.organiser-participates)}))
   ))

(rum/defc edit < rum/reactive
  [{:keys [context quest schema errors]}]
  (let [tr (:tr context)
        cursors-and-schema (c/value-and-error-cursors-and-schema {:for quest
                                                                  :schema schema
                                                                  :errors errors})
        is-valid (atom false)
        checker (partial hs/select-schema-either schema)
        set-valid! (fn [validity] (reset! is-valid validity))
        show-end-time (atom false)
        check-and-set-validity!
        (fn [quest-value]
          (let [value-or-error (checker quest-value)]
            (cond
              (:--value value-or-error) (set-valid! true)
              (:--error value-or-error) (set-valid! false))))]
    (check-and-set-validity! @quest)
    (add-watch
     quest
     ::quest-validator
     (fn [key _ old new] (check-and-set-validity! new)))
    [:form
     {:class "opux-form"
      :on-submit
      (fn [e]
        (.preventDefault e)
        (when (deref is-valid)
          #?(:cljs
             (go
               (let [api-call (if (:id @quest)
                                api/edit-quest
                                api/add-quest)
                     api-quest (assoc @quest :picture-id (str (:picture-id @quest)))
                     from-api (<! (api-call api-quest))]
                 (log/info from-api)
                 )))))
      }
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
      ""
      [:div {:class "opux-fieldset__item opux-fieldset__item--inline-container"}
      (html/button
       (tr [:pages.quest.edit.button.submit])
       {:class "opux-button opux-form__button opux-fieldset__inline-item"
        :type "submit"
        :active is-valid})
      (html/button
       (tr [:pages.quest.edit.button.remove])
       {:class "opux-button opux-form__button opux-fieldset__inline-item"})])]))

(rum/defcs quest-categories-filter < rum/reactive
  (rum/local false ::is-active)
  [state {:keys [cursors-and-schema context tr]}]
  (let [is-active (::is-active state)]
    [:div {:class "opux-card-filter__field opux-card-filter__field--category"}
     [:div {:class "opux-card-filter__label"}
      (tr [:pages.quest.list.filter.category])]
     [:span {:class "opux-icon opux-icon-plus opux-card-filter--category-filter"}]
     (html/form-section
      ""
      (html/multi-selector-for-schema
       {:schema (get-in cursors-and-schema [:categories :schema])
        :value (get-in cursors-and-schema [:categories :value])
        :error (get-in cursors-and-schema [:categories :error])
        :context context}))]))

(rum/defc quest-filters [{:keys [tr cursors-and-schema context]}]
  [:div {:class "opux-content opux-card-filter"}
   (quest-categories-filter {:context context
                             :tr tr
                             :cursors-and-schema cursors-and-schema})

   [:div {:class "opux-card-filter__field opex-card-filter__field--datetime"}
    [:div {:class "opux-card-filter__label"}
     (tr [:pages.quest.list.filter.where])]
    (html/location-selector
     {:class "opux-input opux-input--location-selector"
      :location (get-in cursors-and-schema [:location :value])
      :error (get-in cursors-and-schema [:location :error])
      :schema (get-in cursors-and-schema [:location :schema])
      :placeholder (tr [:pages.quest.edit.location.placeholder])
      :context context})]

   [:div {:class "opux-card-filter__field opex-card-filter__field--datetime"}
    [:div {:class "opux-card-filter__label"}
     (tr [:pages.quest.list.filter.when])]]])

(rum/defc list-quests
  [{:keys [context quests quest-filter schema errors]}]
  (let [tr (:tr context)
        cursors-and-schema
        (c/value-and-error-cursors-and-schema {:for quest-filter
                                               :schema schema
                                               :errors errors})]
    [:div {:class "opux-section"}
     [:h1 (tr [:pages.quest.list.title])]

     (quest-filters {:cursors-and-schema cursors-and-schema
                     :tr tr
                     :context context})

     [:div {:class "opux-card-list-container"}
      [:div
       {:class "opux-content opux-content--small opux-centered opux-card-list__subtitle"}
       [:p (tr [:pages.quest.list.not-found])]]

      [:h2 {:class "opux-centered"}
       "Helmikuussa"]

      [:ul {:class "opux-card-list"}
       (repeat 7 (card))]

      [:h2 {:class "opux-centered"}
       "Maaliskuussa "]

      [:ul {:class "opux-card-list"}
       (repeat 8 (card))]
      ]]))
