(ns hiiop.client-pages
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [clojure.data :as data]
            [cljs.core.async :refer [<!]]
            [rum.core :as rum]
            [taoensso.timbre :as log]
            [schema.core :as schema]
            [hiiop.components.profile :as p-p]
            [hiiop.components.login :as p-l]
            [hiiop.components.activate :as p-a]
            [hiiop.components.register :as p-r]
            [hiiop.components.quest-single :as quest]
            [hiiop.components.quests :as quests]
            [hiiop.client-api :refer [get-quest get-user-info get-own-quests]]
            [hiiop.context :refer [context]]
            [hiiop.mangling :refer [parse-natural-number same-keys-with-nils]]
            [hiiop.mangling :refer [same-keys-with-nils]]
            [hiiop.schema :refer [NewQuest
                                  Quest
                                  EditQuest
                                  RegistrationInfo
                                  UserActivation
                                  QuestFilter
                                  QuestSignup
                                  new-empty-registration-info
                                  new-empty-quest
                                  new-empty-quest-signup-info
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
  (go
    (let [owner (:id (:identity @context))
          quests (<! (get-own-quests))
          user-info (<! (get-user-info owner))]
      (log/info "profile-page")
      (rum/mount
       (p-p/profile {:context @context
                     :user-info user-info
                     :quests (atom quests)})
       (. js/document (getElementById "app"))))))

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
  (go
    (let [id (parse-natural-number
              (get-in params [:route-params :quest-id]))
          quest (<! (get-quest id))]
      (-> quest
          (#(assoc %1
                   :categories
                   (into [] (map keyword (:categories %1)))))
          (atom)
          ((fn [quest] {:quest quest}))
          (#(assoc %1
                   :errors
                   (-> (:quest %1)
                       (deref)
                       (same-keys-with-nils)
                       (atom))))
          (assoc :context @context)
          (assoc :schema EditQuest)
          (#(rum/mount
             (quests/edit %1)
             (. js/document (getElementById "app"))))
          ))))

(defn quest-page [params]
  (let [quest-signup-info (atom (new-empty-quest-signup-info))
        errors (atom (same-keys-with-nils @quest-signup-info))]
    (go
      (let [id (parse-natural-number
                (get-in params [:route-params :quest-id]))
            quest (<! (get-quest id))
            user-info (<! (get-user-info (str (:owner quest))))
            owner-name (:name user-info)]
        (-> quest
            (#(assoc %1
                     :categories
                     (into [] (map keyword (:categories %1)))
                     :owner-name owner-name))
            (atom)
            ((fn [quest]
               {:quest quest}))
            (#(assoc %1
                     :errors
                     (-> (:quest %1)
                         (deref)
                         (same-keys-with-nils)
                         (atom))))
            (assoc :context @context
                   :quest-signup-info quest-signup-info
                   :errors errors
                   :schema QuestSignup)
            (#(rum/mount
               (quest/quest %1)
               (. js/document (getElementById "app"))))
            )))))

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
   :quest
   quest-page
   :browse-quests
   browse-quests-page
   :create-quest
   create-quest-page
   :edit-quest
   edit-quest-page
   })
