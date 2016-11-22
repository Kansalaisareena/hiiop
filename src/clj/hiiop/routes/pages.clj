(ns hiiop.routes.pages
  (:require [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [taoensso.timbre :as log]
            [bidi.ring :refer (make-handler)]
            [hiiop.layout :as layout]
            [hiiop.components.events :as events]
            [hiiop.config :refer [env]]
            [hiiop.routes.page-hierarchy :as page-hierarchy]))

(defn tr-from-req [req]
  (:tempura/tr req))

(defn create-context [req]
  {:tr (tr-from-req req)
   :config env
   :hierarchy page-hierarchy/hierarchy})

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

(defn browse-events [req]
  (let [context (create-context req)
        tr (:tr context)]
    (layout/render {:context context
                    :content (events/list-events {:events ["a" "a" "a"]
                                                  :context context})
                    :title (tr [:actions.events.create])})))


(defn edit-event [req]
  (let [context (create-context req)
        tr (:tr context)]
    (layout/render {:context context
                    :content "Edit event"
                    :title (tr [:actions.events.create])})))

(def handlers
  {:index
   index
   :events-index
   browse-events
   :create-event
   create-event
   :edit-event
   edit-event})

(def ring-handler
  (let [hierarchy page-hierarchy/hierarchy]
    (log/info hierarchy)
    (make-handler
     hierarchy
     (fn [handler]
       (log/info "handler" handler)
       (handler handlers)))))
