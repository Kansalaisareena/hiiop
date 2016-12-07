(ns hiiop.components.quests
  (:require [clojure.string :as str]
            [rum.core :as rum]
            [taoensso.timbre :as log]
            [schema.core :as s]
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

(rum/defc edit [{:keys [context quest schema errors]}]
  (let [tr (:tr context)
        cursors-and-schema (c/value-and-error-cursors-and-schema {:for quest
                                                                  :schema schema
                                                                  :errors errors})]
    (add-watch quest :quest (fn [key _ old new] (log/info new)))
    [:form
     {:on-submit (fn [e] (.preventDefault e))}
     [:h1 (tr [:actions.quest.create])]
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
         :context context})))
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
       (tr [:pages.quest.edit.address])
       {:class "address-label"
        :error (get-in cursors-and-schema [:address :error])}
       (html/input
        {:type "text"
         :value (get-in cursors-and-schema [:address :value])
         :error (get-in cursors-and-schema [:address :error])
         :schema (get-in cursors-and-schema [:address :schema])
         :error-key :error.address
         :context context}))
      (html/label
       (tr [:pages.quest.edit.town])
       {:class "town-label"
        :error (get-in cursors-and-schema [:town :error])}
       (html/input
        {:type "text"
         :value (get-in cursors-and-schema [:town :value])
         :error (get-in cursors-and-schema [:town :error])
         :schema (get-in cursors-and-schema [:town :schema])
         :error-key :error.town
         :context context})))
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
      {:class "organiser-will-participate"
       :id (name :pages.quest.edit.organiser-will-participate)
       :schema (get-in cursors-and-schema [:organiser-will-participate :schema])
       :value (get-in cursors-and-schema [:organiser-will-participate :value])
       :error (get-in cursors-and-schema [:organiser-will-participate :error])
       })
     (html/label
      (tr [:pages.quest.edit.organiser-will-participate])
      {:for (name :pages.quest.edit.organiser-will-participate)}))
     ]))
