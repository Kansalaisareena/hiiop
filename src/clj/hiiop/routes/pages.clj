(ns hiiop.routes.pages
  (:require [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [taoensso.timbre :as log]
            [bidi.ring :refer (make-handler)]
            [hiiop.middleware :refer [authenticated]]
            [hiiop.layout :as layout]
            [hiiop.components.quests :as quests]
            [hiiop.components.login :as l]
            [hiiop.config :refer [env]]
            [hiiop.routes.page-hierarchy :as page-hierarchy]
            [hiiop.mangling :refer [same-keys-with-nils]]
            [hiiop.schema :refer [NewQuest new-empty-quest]]))

(defn tr-from-req [req]
  (:tempura/tr req))

(defn create-context [req]
  {:tr (tr-from-req req)
   :config env
   :hierarchy page-hierarchy/hierarchy
   :identity (:identity req)
   :current-locale (keyword (:current-locale req))})

(defn index [req]
  (let [context (create-context req)
        tr (:tr context)]
    (layout/render {:context context
                    :content (quests/list-quests {:quests ["a" "a" "a"]
                                                  :context context})
                    :title (tr [:pages.index.title])})))

(defn login [req]
  (let [context (create-context req)
        tr (:tr context)]
    (layout/render {:context context
                    :content (l/login {:context context})
                    :title (tr [:pages.index.title])})))

(defn create-quest [req]
  (let [context (create-context req)
        tr (:tr context)
        quest (atom (new-empty-quest))
        errors (atom (same-keys-with-nils @quest))]
    (layout/render {:context context
                    :content
                    (quests/edit {:context context
                                  :quest quest
                                  :schema NewQuest
                                  :errors errors})
                    :title (tr [:actions.quest.create])})))

(defn browse-quests [req]
  (let [context (create-context req)
        tr (:tr context)]
    (layout/render {:context context
                    :content (quests/list-quests {:quests ["a" "a" "a"]
                                                  :context context})
                    :title (tr [:actions.quest.create])})))


(defn edit-quest [req]
  (let [context (create-context req)
        tr (:tr context)]
    (layout/render {:context context
                    :content "Edit event"
                    :title (tr [:actions.quest.edit])})))

(def handlers
  {:index
   index
   :login
   login
   :browse-quests
   browse-quests
   :create-quest
   (authenticated create-quest)
   :edit-quest
   (authenticated create-quest)})

(def ring-handler
  (let [hierarchy page-hierarchy/hierarchy]
    (log/info hierarchy)
    (make-handler
     hierarchy
     (fn [handler]
       (log/info "handler" handler)
       (handler handlers)))))
