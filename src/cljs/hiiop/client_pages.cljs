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
            [hiiop.components.password-reset :refer [request-password-reset
                                                     password-reset]]
            [hiiop.url :as u]
            [hiiop.client-api :refer [get-quest
                                      get-moderated-or-unmoderated-quest
                                      get-secret-quest
                                      get-public-user-info
                                      get-private-user-info
                                      get-user-quests
                                      get-quest-party
                                      joinable-open-quest?
                                      joinable-secret-quest?
                                      get-moderated-quests
                                      get-unmoderated-quests
                                      get-party-member
                                      activate-user
                                      change-password
                                      get-the-counter-days]]
            [hiiop.context :refer [context]]
            [hiiop.mangling :refer [parse-natural-number same-keys-with-nils]]
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
  (rum/mount (password-reset
              {:context @context
               :token (get-in params [:route-params :token])
               :api-fn activate-user
               :done-title :pages.password-reset.done.title
               :done-text :pages.password-reset.done.text
               :done-link (u/url-to "" :login)})
             (. js/document (getElementById "app"))))

(defn index-page []
  (go
    (let [counter-days (:days (<! get-the-counter-days))]
    (rum/mount
      (p-i/index-page {:context @context
                       :category-filter (atom
                                          (new-empty-category-filter))
                       :counter-days counter-days
                       :schema QuestCategoryFilter})
      (. js/document (getElementById "app"))))))

(defn profile-page [params]
  (go
    (let [user-id (:id (:identity @context))
          user-info (<! (get-private-user-info user-id))
          user-quests (<! (get-user-quests))
          participating-quests (:attending user-quests)
          own-quests (:organizing user-quests)
          own-quest-ids (map :id own-quests)
          quests (into [] (concat own-quests
                                  (filter #(not (some #{(:id %)} own-quest-ids))
                                          participating-quests)))]
      (log/info "profile-page")
      (rum/mount
        (p-p/profile {:context @context
                      :user-info user-info
                      :quests (atom quests)})
        (. js/document (getElementById "app"))))))

(defn browse-quests-page [params]
  (go
    (let [quests (filter
                   :is-open
                   (<! (get-moderated-quests)))
          quest-filter (atom (new-empty-quest-filter))
          errors (atom (same-keys-with-nils @quest-filter))
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
        (swap! quest-filter assoc :categories category-queries))

      (rum/mount
        (quest-browse/list-quests {:quests quests
                                   :context @context
                      :quest-filter quest-filter
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
  (go
    (let [quest (atom (new-empty-quest))
          user-id (get-in @context [:identity :id])
          user (<! (get-private-user-info (str user-id)))
          errors (atom (same-keys-with-nils @quest))]
      (rum/mount
        (quests/edit {:context @context
                      :quest quest
                      :user user
                      :errors errors
                      :schema NewQuest})
        (. js/document (getElementById "app"))))))

(defn edit-quest-page [params]
  (go
    (let [id (parse-natural-number
              (get-in params [:route-params :quest-id]))
          quest (<! (get-moderated-or-unmoderated-quest id))
          user-info (<! (get-private-user-info (:owner quest)))
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
              (. js/document (getElementById "app"))))))))

(defn quest-page [params]
  (let [empty-party-member (atom (new-empty-party-member))
        errors (atom (same-keys-with-nils @empty-party-member))]
    (go
      (let [id (parse-natural-number
                 (get-in params [:route-params :quest-id]))
            quest (<! (get-quest id))
            user-info (<! (get-public-user-info (str (:owner quest))))
            joinable (<! (joinable-open-quest? (:id quest)))
            owner-name (:name user-info)]
        (-> quest
            (#(assoc %1
                     :categories
                     (into [] (map keyword (:categories %1)))
                     :owner-name owner-name))
            (atom)
            ((fn [quest]
               {:quest quest
                :joinable joinable}))
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
               (. js/document (getElementById "app")))))))))

(defn secret-quest-page [params]
  (let [empty-party-member (atom (new-empty-party-member))
        errors (atom (same-keys-with-nils @empty-party-member))]
    (go
      (let [id (parse-natural-number
                (get-in params [:route-params :quest-id]))
            secret-party (get-in params [:route-params :secret-party])
            quest (<! (get-secret-quest {:id id
                                         :secret-party secret-party}))
            joinable (<! (joinable-secret-quest? (:id quest) secret-party))
            user-info (<! (get-public-user-info (str (:owner quest))))
            owner-name (:name user-info)]
        (-> quest
            (#(assoc %1
                     :categories
                     (into [] (map keyword (:categories %1)))
                     :owner-name owner-name))
            (atom)
            ((fn [quest]
               {:quest quest
                :joinable joinable}))
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
               (. js/document (getElementById "app")))))))))

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
             (. js/document (getElementById "app"))))))))

(defn request-password-reset-page [params]
  (rum/mount (request-password-reset
              {:context @context})
             (. js/document (getElementById "app"))))

(defn password-reset-page [params]
  (rum/mount (password-reset
              {:context @context
               :token (get-in params [:route-params :token])
               :api-fn change-password
               :done-title :pages.password-reset.done.title
               :done-text :pages.password-reset.done.text
               :done-link (u/url-to "" :login)})
             (. js/document (getElementById "app"))))

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
   :request-password-reset
   request-password-reset-page
   :password-reset
   password-reset-page
   })
