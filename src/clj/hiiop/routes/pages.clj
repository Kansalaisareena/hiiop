(ns hiiop.routes.pages
  (:require [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [taoensso.timbre :as log]
            [bidi.ring :refer (make-handler)]
            [hiiop.layout :as layout]
            [hiiop.components.events :as events]
            [hiiop.config :refer [env]]
            [hiiop.routes.page-hierarchy :as page-hierarchy]))

(defn create-context [req]
  {:tr (:tempura/tr req)
   :config env})

(defn index [req]
  (let [context (create-context req)
        tr (:tr context)]
    (layout/render {:context context
                    :content (events/list-events {:events ["a" "a" "a"]
                                                  :context context})
                    :title (tr [:pages.index.title])})))

(defn create-event [req]
  (let [context (create-context req)
        tr (:tr context)]
    (layout/render {:context context
                    :content (events/create {:context context})
                    :title (tr [:actions.events.create])})))

(def handlers
  {:index
   index
   :events-index
   index
   :create-event
   create-event
   :edit-event
   index})

(def ring-handler
  (let [hierarchy (page-hierarchy/hierarchy handlers)]
    (log/info hierarchy)
    (make-handler hierarchy)))
