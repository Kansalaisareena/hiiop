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

(defn enable-organisation [enable]
  (swap! enable
         #(identity true)))

(rum/defcs edit-content < rum/reactive
                          (rum/local false ::organisation-enabled)
  [state {:keys [context quest schema errors is-valid cursors-and-schema tr]}]
  (let [organisation-enabled (::organisation-enabled state)]
    (html/form-section
     (tr [:pages.quest.edit.subtitles.content])
     (html/label
      (tr [:pages.quest.edit.name])
      {:class "name-label"
       :error (get-in cursors-and-schema [:name :error])}
      (html/input
       {:class "name"
        :type "text"
        :value (get-in cursors-and-schema [:name :value])
        :error (get-in cursors-and-schema [:name :error])
        :schema (get-in cursors-and-schema [:name :schema])
        :context context}))
     (html/label
      (tr [:pages.quest.edit.description])
      {:class "unmoderated-description-label"
       :error (get-in cursors-and-schema [:unmoderated-description :error])}
      (html/text
       {:class "unmoderated-description"
        :value (get-in cursors-and-schema [:unmoderated-description :value])
        :error (get-in cursors-and-schema [:unmoderated-description :error])
        :schema (get-in cursors-and-schema [:unmoderated-description :schema])
        :context context}))
     (html/label
      (tr [:pages.quest.edit.hashtags])
      {:class "hashtags-label"
       :error (get-in cursors-and-schema [:hashtags :error])}
      (html/input
       {:type "text"
        :value (get-in cursors-and-schema [:hashtags :value])
        :error (get-in cursors-and-schema [:hashtags :error])
        :schema (get-in cursors-and-schema [:hashtags :schema])
        :error-key :error.hashtag
        :transform-value #(if (string? %) (str/split % #" ") %)
        :to-value #(if (sequential? %) (str/join " " %) %)
        :context context}))
     (if (not (rum/react organisation-enabled))
       (html/button
        (tr [:pages.quest.edit.button.add-organisation])
        {:class "organisation"
         :on-click (fn [e]
                     (enable-organisation organisation-enabled)
                     (add-organisation-to quest))})
       [:div
        (html/label
         (tr [:pages.quest.edit.organisation.name])
         {:class "organisation-name-label"}
         (html/input
          {:type "text"
           :value (rum/cursor-in quest [:organisation :name])
           :error (atom nil)
           :schema hs/NonEmptyString
           :context context}
          ))
        (html/label
         (tr [:pages.quest.edit.organisation.description])
         {:class "organisation-description-label"}
         (html/text
          {:value (rum/cursor-in quest [:organisation :description])
           :error (atom nil)
           :schema hs/NonEmptyString
           :context context}
          ))])
       )))

(rum/defc edit-time-place < rum/reactive
  [{:keys [quest is-valid cursors-and-schema context tr]}]
  (html/form-section
   (tr [:pages.quest.edit.subtitles.time-place])
   (html/label
    (tr [:pages.quest.edit.start-time])
    {:class "start-time-label"
     :error (atom nil)}
    (html/datetime-picker
     {:date (get-in cursors-and-schema [:start-time :value])
      :error (get-in cursors-and-schema [:start-time :error])
      :schema (get-in cursors-and-schema [:start-time :schema])
      :max-date (get-in cursors-and-schema [:end-time :value])
      :class "start-time"
      :value-format time/transit-format
      :date-print-format time/date-print-format
      :time-print-format time/time-print-format
      :context context}))
   (html/label
    (tr [:pages.quest.edit.end-time])
    {:class "end-time-label"
     :error (atom nil)}
    (html/datetime-picker
     {:date (get-in cursors-and-schema [:end-time :value])
      :error (get-in cursors-and-schema [:end-time :error])
      :schema (get-in cursors-and-schema [:end-time :schema])
      :min-date (get-in cursors-and-schema [:start-time :value])
      :class "end-time"
      :value-format time/transit-format
      :date-print-format time/date-print-format
      :time-print-format time/time-print-format
      :context context}))
   (html/label
    (tr [:pages.quest.edit.location])
    {:class "location-label"
     :error (get-in cursors-and-schema [:location :error])}
    (html/location-selector
     {:location (get-in cursors-and-schema [:location :value])
      :error (get-in cursors-and-schema [:location :error])
      :schema (get-in cursors-and-schema [:location :schema])
      :context context}))
   ))

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
     {:on-submit (fn [e] (.preventDefault e))}
     [:h1 (tr [:actions.quest.create])]
     (edit-content
      {:cursors-and-schema cursors-and-schema
       :is-valid is-valid
       :context context
       :quest quest
       :tr tr
       })
     (edit-time-place
      {:cursors-and-schema cursors-and-schema
       :is-valid is-valid
       :context context
       :quest quest
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
       {:class "submit"
        :active is-valid})
      (html/button
       (tr [:pages.quest.edit.button.remove])
       {:class "cancel"}))
     ]))
