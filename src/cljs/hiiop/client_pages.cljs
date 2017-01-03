(ns hiiop.client-pages
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [clojure.data :as data]
            [cljs.core.async :refer [<!]]
            [rum.core :as rum]
            [taoensso.timbre :as log]
            [schema.core :as schema]
            [hiiop.components.profile :as p-p]
            [hiiop.components.login :as p-l]
            [hiiop.components.index :as p-i]
            [hiiop.components.activate :as p-a]
            [hiiop.components.register :as p-r]
            [hiiop.components.quest-single :as quest-single]
            [hiiop.components.quests :as quests]
            [hiiop.components.moderate :as moderate]
            [hiiop.components.part-party :refer [part-party]]
            [hiiop.components.quests-browse :as quest-browse]
            [hiiop.client-api :refer [get-quest
                                      get-secret-quest
                                      get-user-info
                                      get-own-quests
                                      get-participating-quests
                                      get-quest-party
                                      get-moderated-quests
                                      get-unmoderated-quests
                                      get-party-member]]
            [hiiop.context :refer [context]]
            [hiiop.mangling :refer [parse-natural-number same-keys-with-nils]]
            [hiiop.mangling :refer [same-keys-with-nils]]
            [hiiop.schema :refer [NewQuest
                                  Quest
                                  EditQuest
                                  RegistrationInfo
                                  UserActivation
                                  QuestFilter
                                  QuestCategoryFilter
                                  NewPartyMember
                                  new-empty-registration-info
                                  new-empty-quest
                                  new-empty-party-member
                                  new-empty-quest-filter
                                  new-empty-category-filter
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

(defn index-page []
  (log/info "index-page")
  (rum/mount
    (p-i/index-page {:context @context
                     :category-filter (atom
                                        (new-empty-category-filter))
                     :schema QuestCategoryFilter})
    (. js/document (getElementById "app"))))

(defn profile-page [params]
  (go
    (let [owner (:id (:identity @context))
          user-info (<! (get-user-info owner))
          own-quests (<! (get-own-quests))
          participating-quests (<! (get-participating-quests))
          quests (into []
                       (distinct
                         (flatten [participating-quests own-quests])))]
      (log/info "profile-page")
      (rum/mount
        (p-p/profile {:context @context
                      :user-info user-info
                      :quests (atom quests)})
        (. js/document (getElementById "app"))))))

(defn browse-quests-page [params]
  (go
    (let [quests (<! (get-moderated-quests))
          quest-filter (atom (new-empty-quest-filter))
          errors (atom (same-keys-with-nils @quest-filter))
          filtered-quests (atom quests)
          category-queries (-> js/location
                               (.-hash)
                               (clojure.string/replace #"[#\?\&]" "")
                               (clojure.string/split #"categories\[]\=")
                               (rest)
                               (#(map keyword %1))
                               (#(into [] %1)))]
      (log/info "browse-quests-page")

      ;; Filter quests categories from url hash
      (when (not-empty category-queries)
        (swap! quest-filter assoc :categories category-queries)
        (reset! filtered-quests
                (quest-browse/filters {:quests quests
                                       :quest-filter @quest-filter})))

      (rum/mount
        (quest-browse/list-quests {:quests quests
                                   :context @context
                      :quest-filter quest-filter
                      :filtered-quests filtered-quests
                      :errors errors
                      :schema QuestFilter})
        (. js/document (getElementById "app"))))))

(defn moderate-page [params]
  (go
    (let [moderated-quests (<! (get-moderated-quests))
          unmoderated-quests (<! (get-unmoderated-quests))]
      (rum/mount
        (moderate/moderate-page {:context @context
                                 :unmoderated-quests (atom unmoderated-quests)
                                 :moderated-quests (atom moderated-quests)})
        (. js/document (getElementById "app"))))))



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
          quest (<! (get-quest id))
          user-info (<! (get-user-info (:owner quest)))
          party (<! (get-quest-party id))]
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
          (assoc :user user-info)
          (assoc :context @context)
          (assoc :schema EditQuest)
          (assoc :party (atom party))
          (#(rum/mount
              (quests/edit %1)
              (. js/document (getElementById "app"))))
          ))))

(defn quest-page [params]
  (let [empty-party-member (atom (new-empty-party-member))
        errors (atom (same-keys-with-nils @empty-party-member))]
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
                   :empty-party-member empty-party-member
                   :party-member-errors errors
                   :party-member-schema NewPartyMember)
            (#(rum/mount
               (quest-single/quest %1)
               (. js/document (getElementById "app"))))
            )))))

(defn secret-quest-page [params]
  (let [empty-party-member (atom (new-empty-party-member))
        errors (atom (same-keys-with-nils @empty-party-member))]
    (go
      (let [id (parse-natural-number
                (get-in params [:route-params :quest-id]))
            secret-party (get-in params [:route-params :secret-party])
            quest (<! (get-secret-quest {:id id
                                         :secret-party secret-party}))
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
                   :empty-party-member empty-party-member
                   :party-member-errors errors
                   :party-member-schema NewPartyMember
                   :secret-party secret-party)
            (#(rum/mount
                (quest-single/quest %1)
                (. js/document (getElementById "app"))))
            )))))

(defn part-quest-party-page [params]
  (go
    (let [member-id (get-in params [:route-params :member-id])
          quest-id (get-in params [:route-params :quest-id])
          quest (<! (get-quest quest-id))
          party-member (<! (get-party-member {:quest-id quest-id
                                              :member-id member-id}))]
      (-> (assoc {} :context @context)
          (assoc :quest quest)
          (assoc :party-member party-member)
          (#(rum/mount
             (part-party %1)
             (. js/document (getElementById "app")))))
      )))

(def handlers
  {:index
   index-page
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
   :secret-quest
   secret-quest-page
   :part-quest-party
   part-quest-party-page
   :browse-quests
   browse-quests-page
   :create-quest
   create-quest-page
   :edit-quest
   edit-quest-page
   :moderate
   moderate-page
   })
