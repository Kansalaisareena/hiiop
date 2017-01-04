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
            #?(:cljs [hiiop.scroll :refer [scroll-top]])
            [hiiop.url :refer [redirect-to]]
            [hiiop.time :as time]
            [hiiop.components.core :as c]
            [hiiop.components.quest-single :as qs]
            [hiiop.components.quest-card :refer [quest-card-browse]]
            [hiiop.html :as html]
            [hiiop.schema :as hs]))

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
         (tr [:pages.quest.edit.picture.title])
         {:class "opux-input__label opux-input__label--picture-label"
          :error (get-in cursors-and-schema [:picture-id :error])})
        (html/file-input
         {:value (get-in cursors-and-schema [:picture-id :value])
          :error (get-in cursors-and-schema [:picture-id :error])
          :url picture-url
          :context context
          :tr (partial tr [:pages.quest.edit.picture.upload-failed])})
        [:p {:class "opux-info opux-centered"} (tr [:pages.quest.edit.picture.info])]
        ]
       [[:img {:src @picture-url
               :on-click
               (fn []
                 #?(:cljs
                    (do
                      (if (js/confirm (tr [:pages.quest.edit.picture.remove]))
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
        :error-key :errors.hashtag
        :transform-value #(if (string? %) (str/split % #" ") %)
        :to-value #(if (sequential? %) (str/join " " %) %)
        :context context})
      ]
     (if (not (rum/react organisation-enabled))
       (html/button
        (tr [:pages.quest.edit.button.add-organisation])
        {:class "opux-button"
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
         [:div
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
           )]])))))

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
     :choice-name-fn hs/category-choice
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
     [:div {:class "opux-fieldset__item"}
      (html/checkbox-binary
       {:class "organiser-participates opux-fieldset__item"
        :id (name :pages.quest.edit.organiser-will-participate)
        :schema (get-in cursors-and-schema [:organiser-participates :schema])
        :value (get-in cursors-and-schema [:organiser-participates :value])
        :error (get-in cursors-and-schema [:organiser-participates :error])
        }
       )
      (html/label
       (tr [:pages.quest.edit.organiser-will-participate])
       {:class "opux-input__label opux-input__label--checkbox"
        :for (name :pages.quest.edit.organiser-will-participate)})])
   ))

(rum/defc confirm-remove < rum/reactive
  [{:keys [ask-confirm
           remove-confirmed
           context]}]
  (let [tr (:tr context)]
    [:div {:class "opux-content"}
     [:h3 {:class "opux-centered"}
      (tr [:pages.quest.edit.remove.title])]
     [:div {:class "opux-fieldset__item opux-fieldset__item--inline-container"}
      (html/button
       (tr [:pages.quest.edit.remove.cancel])
       {:class "opux-button opux-button--dull opux-form__button opux-fieldset__inline-item"
        :type "button"
        :on-click
        (fn []
          (reset! ask-confirm false)
          (reset! remove-confirmed false))
        })
      (html/button
       (tr [:pages.quest.edit.remove.confirm])
       {:class "opux-button opux-form__button opux-fieldset__inline-item opux-button--highlight"
        :type "button"
        :on-click
        (fn []
          (reset! remove-confirmed true))
        })
      ]]
     ))

(rum/defc edit-buttons < rum/reactive
  [{:keys [is-valid ask-remove context]}]
  (let [tr (:tr context)]
    [:div {:class "opux-fieldset__item opux-fieldset__item--inline-container"}
     (html/button
      (tr [:pages.quest.edit.button.preview])
      {:class "opux-button opux-form__button opux-fieldset__inline-item opux-button--highlight"
       :type "submit"
       :active is-valid})
     (html/button
      (tr [:pages.quest.edit.button.remove])
      {:class "opux-button opux-button--dull opux-form__button opux-fieldset__inline-item"
       :on-click
       (fn []
         (reset! ask-remove true))
       })]))

(defn delete-or-buttons!
  [{:keys [ask-remove remove-confirmed quest is-valid context]}]
  (cond
    (and @ask-remove @remove-confirmed)
    (do
      #?(:cljs
         (if (:id @quest)
           (go
             (let [deleted (<! (api/delete-quest (:id @quest)))]
               (when deleted
                 (redirect-to {:path-key :profile}))
               ))
           (redirect-to {:path-key :profile}))
         )
      []
      )

    @ask-remove
    (confirm-remove
     {:ask-confirm ask-remove
      :remove-confirmed remove-confirmed
      :context context})

    :else
    (edit-buttons
     {:ask-remove ask-remove
      :is-valid is-valid
      :context context})
    ))

(rum/defc party-member-confirm-remove
  [{:keys [party context processing confirm member-id quest-id]}]
  (let [tr (:tr context)]
    [:div
     [:span
      {:class "opux-button opux-button--underline opux-button--small"
       :type "button"
       :on-click
       (fn [e]
         #?(:cljs
            (go
              (let [removed (<! (api/remove-party-member
                                {:quest-id quest-id
                                 :member-id member-id}))]
                (when removed
                  (do
                    (reset! party (<! (api/get-quest-party quest-id)))
                    (reset! processing true))))
              )
            ))
       }
      (tr [:pages.quest.edit.party.confirm-remove])]
     [:span
      {:class "opux-button opux-button--underline opux-button--small"
       :type "button"
       :on-click
       (fn []
         (reset! confirm false))
       }
      (tr [:pages.quest.edit.party.cancel-remove])
      ]
     ]
    )
  )

(rum/defcs edit-party-member < rum/reactive
                             < (rum/local false ::confirm)
  [state {:keys [quest party context processing]} {:keys [member-id name email phone days]}]
  (log/info quest @party)
  (let [tr (:tr context)
        confirm (::confirm state)]
    [:tr {:class "opux-table__row"}
     [:td {:class "opux-table__data opux-table__data--name"} name]
     [:td {:class "opux-table__data opux-table__data--email"} email]
     [:td {:class "opux-table__data opux-table__data--phone"} phone]
     [:td {:class "opux-table__data opux-table__data--phone"} days]
     [:td {:class "opux-table__data opux-table__data--actions"}
      (if (rum/react confirm)
        (party-member-confirm-remove
         {:context context
          :party party
          :confirm confirm
          :quest-id (:id quest)
          :member-id member-id
          :processing processing})
        [:button
         {:class "opux-button--icon opux-icon opux-icon-trashcan"
          :type "button"
          :on-click
          (fn [e]
            (reset! confirm true)
            )}]
        )]]))

(rum/defcs edit-party < rum/reactive
                        (rum/local false ::processing)
  [state {:keys [quest party context]}]
  (let [tr (:tr context)
        processing (::processing state)
        edit-member (partial edit-party-member {:context context
                                                :quest quest
                                                :party party
                                                :processing processing})]
    [:div {:class "opux-form-section opux-form-section--no-border"}
     [:h2 {:class "opux-centered"}
      (tr [:pages.quest.edit.party.title])]
     (if (not-empty (rum/react party))
       [:table
        {:class "opux-table opux-centered opux-content"}
        (into [:tbody {:class "opux-table__body"}] (map edit-member @party))]
       [:p {:class "opux-content opux-centered"} (tr [:pages.quest.edit.party.empty])])]))

(rum/defcs edit-form < rum/reactive
                       (rum/local false ::ask-remove)
                       (rum/local false ::remove-confirmed)
  [state {:keys [context quest schema party errors view is-valid]}]
  (let [tr (:tr context)
        cursors-and-schema (c/value-and-error-cursors-and-schema {:for quest
                                                                  :schema schema
                                                                  :errors errors})
        checker (partial hs/select-schema-either schema)
        set-valid! (fn [validity]
                     (if (not (= validity @is-valid))
                       (reset! is-valid validity)
                       validity))
        show-end-time (atom false)
        check-and-set-validity!
        (fn [quest-value]
          (let [value-or-error (checker quest-value)]
            (cond
              (:--value value-or-error) (set-valid! true)
              (:--error value-or-error) (set-valid! false))))
        ask-remove (::ask-remove state)
        remove-confirmed (::remove-confirmed state)
        buttons (delete-or-buttons!
                 {:quest quest
                  :ask-remove ask-remove
                  :remove-confirmed remove-confirmed
                  :is-valid is-valid
                  :context context})]
    (check-and-set-validity! @quest)
    (add-watch quest
               ::quest-validator
               (fn [key _ old new] (check-and-set-validity! new)))
    [:form
     {:class "opux-form"
      :on-submit
      (fn [e]
        (.preventDefault e)
        (reset! view "preview"))
      }
     [:h1 {:class "opux-centered"} (tr [:actions.quest.create])]
     (edit-content
      {:cursors-and-schema cursors-and-schema
       :is-valid is-valid
       :context context
       :quest quest
       :errors errors
       :tr tr
       })
     [:div {:class "opux-line opux-content"}]

     (edit-time-place
      {:cursors-and-schema cursors-and-schema
       :is-valid is-valid
       :context context
       :quest quest
       :errors errors
       :tr tr
       })
     [:div {:class "opux-line opux-content"}]

     (html/form-section
      (tr [:pages.quest.edit.subtitles.related-to])
      (html/multi-selector-for-schema
       {:schema (get-in cursors-and-schema [:categories :schema])
        :value (get-in cursors-and-schema [:categories :value])
        :error (get-in cursors-and-schema [:categories :error])
        :choice-name-fn hs/category-choice
        :context context})
      )
     [:div {:class "opux-line opux-content"}]

     (edit-participation-settings
      {:cursors-and-schema cursors-and-schema
       :is-valid is-valid
       :context context
       :quest quest
       :tr tr
       })
     (when (:id (rum/react quest))
       (edit-party {:quest @quest
                    :party party
                    :context context}))

     (html/form-section
      ""
      buttons
      )]))

(rum/defc preview
  [{:keys [context quest user schema errors view is-valid]}]
  (let [tr (:tr context)]
    [:div
     {:class "opux-content"}
     [:h1 {:class "opux-centered"}
      (tr [:pages.quest.preview.title])]
     [:div {:class "opux-fieldset__item opux-fieldset__item--inline-container"}
      (html/button
       (tr [:pages.quest.preview.buttons.edit])
       {:class "opux-button opux-form__button opux-fieldset__inline-item"
        :on-click
        (fn []
          (reset! view "edit"))})
      (html/button
       (tr [:pages.quest.preview.buttons.publish])
       {:class "opux-button opux-button--highlight opux-form__button opux-fieldset__inline-item"
        :type "submit"
        :active is-valid
        :on-click
        (fn []
          (when @is-valid
            #?(:cljs
               (go
                 (let [api-call (if (:id @quest)
                                  api/edit-quest
                                  api/add-quest)
                       api-quest (-> @quest
                                     (assoc :picture-id (str (:picture-id @quest)))
                                     (dissoc :participant-count))
                       from-api (<! (api-call api-quest))]
                   (if (:success from-api)
                     (reset! view "success"))
                   )))))})
      ]
     (qs/quest {:quest quest :context context :user user})
     ]))

(rum/defc edit-success < rum/reactive
  [{:keys [context quest schema errors view is-valid]}]
  (let [tr (:tr context)
        edit (:id @quest)
        title (if edit
                :pages.quest.edited.title
                :pages.quest.created.title)
        content (if edit
                  :pages.quest.edited.content
                  (cond
                    (:is-open @quest) :pages.quest.created.public
                    :else :pages.quest.created.private))]
    [:div {:class "opux-content"}
     [:h1 (tr [title])]
     [:p (tr [content])]]
    ))

(rum/defcs edit < rum/reactive
                  (rum/local "edit" ::view)
                  (rum/local false ::is-valid)
  [state {:keys [context quest party schema errors user] :as args}]
  (let [view (::view state)
        is-valid (::is-valid state)
        locals {:view view :is-valid is-valid}]
    (cond
      (= @view "edit")    (edit-form (conj args locals))
      (= @view "preview") (let [p (preview (conj args locals))]
                            #?(:cljs (scroll-top))
                            p)
      (= @view "success") (edit-success {:quest quest
                                         :context context})
      )))
