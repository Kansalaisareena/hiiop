(ns hiiop.client-pages
  (:require [clojure.data :as data]
            [rum.core :as rum]
            [taoensso.timbre :as log]
            [schema.core :as schema]
            [hiiop.components.profile :as p-p]
            [hiiop.components.login :as p-l]
            [hiiop.components.activate :as p-a]
            [hiiop.components.register :as p-r]
            [hiiop.components.quests :as quests]
            [hiiop.context :refer [context]]
            [hiiop.mangling :refer [same-keys-with-nils]]
            [hiiop.schema :refer [NewQuest
                                  Quest
                                  RegistrationInfo
                                  UserActivation
                                  QuestFilter
                                  new-empty-registration-info
                                  new-empty-quest
                                  new-empty-quest-filter
                                  new-empty-activation-info
                                  ]]
            [clojure.string :as string]))

(defn login-page [params]
  (log/info "login-page")
  (rum/mount
   (p-l/login {:context @context})
   (. js/document (getElementById "app"))))

(defn register-page [params]
  (let [registration-info (atom (new-empty-registration-info))
        errors (atom (same-keys-with-nils @registration-info))]
    (log/info "register-page" @registration-info (new-empty-registration-info))
    (rum/mount
     (p-r/register {:context @context
                    :registration-info registration-info
                    :schema RegistrationInfo
                    :errors errors})
     (. js/document (getElementById "app")))))

(defn activate-page [params]
  (let [activation-info (atom (new-empty-activation-info))
        errors (atom (same-keys-with-nils @activation-info))]
    (log/info "register-page" @activation-info (new-empty-activation-info))
    (rum/mount
     (p-a/activate {:context @context
                    :token (last (string/split
                                  (.-pathname (.-location js/window)) #"/"))
                    :activation-info activation-info
                    :schema UserActivation
                    :errors errors})
     (. js/document (getElementById "app")))))

(defn profile-page [params]
  (log/info "profile-page")
  (rum/mount
   (p-p/profile {:context @context
                 :quests ["a" "b" "c" "d"]})
   (. js/document (getElementById "app"))))


(defn browse-quests-page [params]
  (let [quest-filter (atom (new-empty-quest-filter))
        errors (atom (same-keys-with-nils @quest-filter))]
    (log/info "browse-quests-page")
    (rum/mount
     (quests/list-quests {:quests ["a" "b" "c" "d"]
                          :context @context
                          :quest-filter quest-filter
                          :errors errors
                          :schema QuestFilter})
     (. js/document (getElementById "app")))))

(defn create-quest-page [params]
  (let [quest (atom (new-empty-quest))
        errors (atom (same-keys-with-nils @quest))]
    (rum/mount
     (quests/edit {:context @context
                   :quest quest
                   :errors errors
                   :schema NewQuest})
     (. js/document (getElementById "app")))))

(defn edit-quest-page [params]
  (let [quest (atom (new-empty-quest))
        errors (atom (same-keys-with-nils @quest))]
    (log/info "edit-quest-page" @quest (new-empty-quest))
    (rum/mount
     (quests/edit {:context @context
                   :quest quest
                   :errors errors
                   :schema Quest})
     (. js/document (getElementById "app")))))

(def handlers
  {:index
   browse-quests-page
   :login
   login-page
   :profile
   profile-page
   :register
   register-page
   :activate
   activate-page
   :browse-quests
   browse-quests-page
   :create-quest
   create-quest-page
   :edit-quest
   edit-quest-page
   })
