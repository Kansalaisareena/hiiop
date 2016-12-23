(ns hiiop.routes.pages
  (:require [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [taoensso.timbre :as log]
            [bidi.ring :refer (make-handler)]
            [hiiop.middleware :refer [authenticated]]
            [hiiop.layout :as layout]
            [hiiop.components.profile :as p-p]
            [hiiop.components.quest-single :as quest]
            [hiiop.components.quests :as quests]
            [hiiop.components.quests-browse :as p-b]
            [hiiop.components.activate :as p-a]
            [hiiop.components.register :as p-r]
            [hiiop.components.errors :as e]
            [hiiop.components.login :as p-l]
            [hiiop.config :refer [env]]
            [hiiop.routes.page-hierarchy :refer [hierarchy]]
            [hiiop.url :refer [redirect-to]]
            [hiiop.mangling :refer [parse-natural-number
                                    same-keys-with-nils]]
            [hiiop.schema :refer [Quest
                                  EditQuest
                                  NewQuest
                                  RegistrationInfo
                                  UserActivation
                                  QuestFilter
                                  NewPartyMember
                                  new-empty-quest
                                  new-empty-party-member
                                  new-empty-quest-filter
                                  new-empty-registration-info
                                  new-empty-activation-info]]
            [hiiop.api-handlers :refer [get-quest
                                        get-secret-quest
                                        get-user
                                        get-quests-for-owner
                                        get-quest-party
                                        get-moderated-quests]]))

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
        quests (get-moderated-quests)
        quest-filter (atom (new-empty-quest-filter))
        errors (atom (same-keys-with-nils @quest-filter))]
    (layout/render {:context context
                    :content (p-b/list-quests {:quests quests
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
        owner (:id (:identity context))
        user-info (get-user owner)
        quests (get-quests-for-owner (:id (:identity context)))
        tr (:tr context)]
    (layout/render {:context context
                    :content (p-p/profile {:context context
                                           :user-info user-info
                                           :quests (atom quests)})})))

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
        quests (get-moderated-quests)
        tr (:tr context)]
    (layout/render {:context context
                    :content (p-b/list-quests {:quests quests
                                               :context context})
                    :title (tr [:actions.quest.create])})))

(defn edit-quest-with-schema [{:keys [request schema quest party title-key]}]
  (let [context (create-context request)
        tr (:tr context)
        quest-atom (atom quest)
        party-atom (atom party)
        errors (atom (same-keys-with-nils @quest-atom))]
    (layout/render {:title (tr [])
                    :context context
                    :content
                    (quests/edit {:context context
                                  :quest quest-atom
                                  :party party-atom
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
        quests (get-moderated-quests)
        quest-filter (atom (new-empty-quest-filter))
        errors (atom (same-keys-with-nils @quest-filter))]
    (layout/render {:context context
                    :content (p-b/list-quests {:quests quests
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
        identity (:identity req)
        quest (get-quest (parse-natural-number id))
        owner? (= (:owner quest) (:id identity))
        party (vec (get-quest-party
                    {:quest-id (:id id)
                     :user identity}))
        context (create-context req)
        tr (:tr context)]
    (if (and owner? quest)
      (edit-quest-with-schema
       {:request req
        :schema EditQuest
        :quest quest
        :title-key :actions.quest.edit
        :party party})
      (redirect-to {:path-key :index}))))

(defn quest [req]

  (let [id (get-in req [:params :quest-id])
        quest (get-quest (parse-natural-number id))
        empty-party-member (atom (new-empty-party-member))
        errors (atom (same-keys-with-nils @empty-party-member))
        owner-name (:name (get-user (:owner quest)))
        context (create-context req)
        tr (:tr context)]
    (if quest
      (layout/render {:title (tr [:actions.quest.create])
                      :context context
                      :content
                      (quest/quest {:context context
                                    :quest (atom (assoc quest
                                                        :owner-name owner-name))
                                    :empty-party-member empty-party-member
                                    :party-member-errors errors
                                    :party-member-schema NewPartyMember})}))))

(defn secret-quest [req]
  (let [id (get-in req [:params :quest-id])
        secret-party (get-in req [:params :secret-party])
        quest (get-secret-quest
               {:id (parse-natural-number id)
                :secret-party secret-party})
        empty-party-member (atom
                            (-> (new-empty-party-member)
                                (assoc :secret-party secret-party)))
        errors (atom (same-keys-with-nils @empty-party-member))
        owner-name (:name (get-user (:owner quest)))
        context (create-context req)
        tr (:tr context)]
    (if quest
      (layout/render {:title (tr [:actions.quest.create])
                      :context context
                      :content
                      (quest/quest {:context context
                                    :quest (atom (assoc quest
                                                        :owner-name owner-name))
                                    :empty-party-member empty-party-member
                                    :party-member-errors errors
                                    :party-member-schema NewPartyMember})}))))

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
   :quest
   quest
   :secret-quest
   secret-quest
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
