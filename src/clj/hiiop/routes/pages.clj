(ns hiiop.routes.pages
  (:require [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [taoensso.timbre :as log]
            [bidi.ring :refer (make-handler)]
            [hiiop.middleware :refer [authenticated]]
            [hiiop.layout :as layout]
            [hiiop.components.profile :as p-p]
            [hiiop.components.quests :as quests]
            [hiiop.components.activate :as p-a]
            [hiiop.components.register :as p-r]
            [hiiop.components.login :as p-l]
            [hiiop.config :refer [env]]
            [hiiop.routes.page-hierarchy :refer [hierarchy]]
            [hiiop.mangling :refer [parse-natural-number same-keys-with-nils]]
            [hiiop.schema :refer [Quest
                                  NewQuest
                                  RegistrationInfo
                                  UserActivation
                                  QuestFilter
                                  new-empty-quest
                                  new-empty-quest-filter
                                  new-empty-registration-info
                                  new-empty-activation-info]]
            [hiiop.api-handlers :refer [get-quest]]))

(defn tr-from-req [req]
  (:tempura/tr req))

(defn create-context [req]
  {:tr (tr-from-req req)
   :config env
   :hierarchy hierarchy
   :identity (:identity req)
   :current-locale (keyword (:current-locale req))})

(defn index [req]
  (let [context (create-context req)
        tr (:tr context)
        quest-filter (atom (new-empty-quest-filter))
        errors (atom (same-keys-with-nils @quest-filter))]
    (layout/render {:context context
                    :content (quests/list-quests {:quests ["a" "a" "a"]
                                                  :quest-filter quest-filter
                                                  :context context
                                                  :schema QuestFilter})
                    :title (tr [:actions.quest.create])

                    :scripts
                    [(str
                      "https://maps.googleapis.com/maps/api/js?"
                      "key=AIzaSyDfXn9JTGue0fbkI3gqIqe7_WUn0M-dt-8"
                      "&libraries=places"
                      "&language=" "fi" ;; to normalize the google data
                      "&region=FI"
                      )]
                    })))

(defn login [req]
  (let [context (create-context req)
        tr (:tr context)]
    (layout/render {:context context
                    :content (p-l/login {:context context})
                    :title (tr [:pages.login.title])})))

(defn register [req]
  (let [context (create-context req)
        registration-info (atom (new-empty-registration-info))
        errors (atom (same-keys-with-nils @registration-info))
        tr (:tr context)]
    (layout/render {:context context
                    :content (p-r/register {:context context
                                            :registration-info registration-info
                                            :schema RegistrationInfo
                                            :errors errors})
                    :title (tr [:pages.register.title])})))

(defn profile [req]
  (let [context (create-context req)
        tr (:tr context)]
    (layout/render {:context context
                    :content (p-p/profile {:context context
                                           :quests ["a" "b" "c" "d"]})})))

(defn activate [req]
  (let [context (create-context req)
        tr (:tr context)
        activation-info (atom (new-empty-activation-info))
        errors (atom (same-keys-with-nils @activation-info))
        route-params (:route-params req)
        token (:token route-params)]
    (layout/render {:context context
                    :content (p-a/activate {:context context
                                            :activation-info activation-info
                                            :token token
                                            :schema UserActivation
                                            :errors errors})
                    :title (tr [:pages.activate.title])})))

(defn browse-quests [req]
  (let [context (create-context req)
        tr (:tr context)]
    (layout/render {:context context
                    :content (quests/list-quests {:quests ["a" "a" "a"]
                                                  :context context})
                    :title (tr [:actions.quest.create])})))

(defn edit-quest-with-schema [{:keys [request schema quest title-key]}]
  (let [context (create-context request)
        tr (:tr context)
        quest-atom (atom quest)
        errors (atom (same-keys-with-nils @quest-atom))]
    (layout/render {:title (tr [])
                    :context context
                    :content
                    (quests/edit {:context context
                                  :quest quest-atom
                                  :schema schema
                                  :errors errors})
                    :scripts
                    [(str
                      "https://maps.googleapis.com/maps/api/js?"
                      "key=AIzaSyDfXn9JTGue0fbkI3gqIqe7_WUn0M-dt-8"
                      "&libraries=places"
                      "&language=" "fi" ;; to normalize the google data
                      "&region=FI"
                      )]
                    })))

(defn browse-quests [req]
  (let [context (create-context req)
        tr (:tr context)
        quest-filter (atom (new-empty-quest-filter))
        errors (atom (same-keys-with-nils @quest-filter))]
    (layout/render {:context context
                    :content (quests/list-quests {:quests ["a" "a" "a"]
                                                  :quest-filter quest-filter
                                                  :context context
                                                  :schema QuestFilter})
                    :title (tr [:actions.quest.create])
                    :scripts
                    [(str
                      "https://maps.googleapis.com/maps/api/js?"
                      "key=AIzaSyDfXn9JTGue0fbkI3gqIqe7_WUn0M-dt-8"
                      "&libraries=places"
                      "&language=" "fi" ;; to normalize the google data
                      "&region=FI"
                      )]
                    })))

(defn create-quest [req]
  (edit-quest-with-schema
   {:request req
    :schema NewQuest
    :quest (new-empty-quest)
    :title-key :actions.quest.create}))

(defn edit-quest [req]
  (let [id (get-in req [:params :quest-id])
        quest (get-quest (parse-natural-number id))]
    (if quest
      (edit-quest-with-schema
       {:request req
        :schema Quest
        :quest quest
        :title-key :actions.quest.edit}))))

(def handlers
  {:index
   index
   :login
   login
   :register
   register
   :activate
   activate
   :profile
   (authenticated profile)
   :browse-quests
   browse-quests
   :create-quest
   (authenticated create-quest)
   :edit-quest
   (authenticated edit-quest)})

(def ring-handler
  (do
    (log/info hierarchy)
    (make-handler
     hierarchy
     (fn [handler]
       (log/info "handler" handler)
       (handler handlers)))))
