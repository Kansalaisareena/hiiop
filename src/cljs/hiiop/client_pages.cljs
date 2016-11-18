(ns hiiop.client-pages
  (:require [rum.core :as rum]
            [hiiop.components.events :as events]
            [hiiop.context :refer [context]]
            [taoensso.timbre :as log]))

(defn list-events-page [params]
  (log/info "list-events-page")
  (rum/mount
   (events/list-events {:events ["a" "b" "c" "d"]
                        :context @context})
   (. js/document (getElementById "app"))))

(defn create-event-page [params]
  (log/info "create-event-page")
  (rum/mount
   (events/create {:context @context})
   (. js/document (getElementById "app"))))

(def handlers
  {:index
   list-events-page
   :events-index
   list-events-page
   :create-event
   create-event-page
   :edit-event
   list-events-page
   })
