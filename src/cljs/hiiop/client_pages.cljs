(ns hiiop.client-pages
  (:require [clojure.data :as data]
            [rum.core :as rum]
            [taoensso.timbre :as log]
            [schema.core :as schema]
            [hiiop.components.login :as p-l]
            [hiiop.components.activate :as p-a]
            [hiiop.components.register :as p-r]
            [hiiop.components.quests :as quests]
            [hiiop.context :refer [context]]
            [hiiop.mangling :refer [same-keys-with-nils]]
            [hiiop.schema :refer [new-empty-quest NewQuest Quest RegistrationInfo new-empty-registration-info]]))

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
  (log/info "register-page")
  (rum/mount
   (p-a/activate {:context @context})
   (. js/document (getElementById "app"))))

(defn browse-quests-page [params]
  (log/info "browse-quests-page")
  (rum/mount
   (quests/list-quests {:quests ["a" "b" "c" "d"]
                        :context @context})
   (. js/document (getElementById "app"))))

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
