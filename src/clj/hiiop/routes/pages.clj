(ns hiiop.routes.pages
  (:require [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [taoensso.timbre :as log]
            [schema.coerce :as sc]
            [bidi.ring :refer (make-handler)]
            [bidi.bidi :refer [path-for match-route]]
            [hiiop.middleware :refer [authenticated]]
            [hiiop.layout :as layout]
            [hiiop.components.moderate :as p-m]
            [hiiop.components.profile :as p-p]
            [hiiop.components.quest-single :as quest]
            [hiiop.components.quest-card :refer [get-quest-image]]
            [hiiop.components.quests :as quests]
            [hiiop.components.index :as index-page]
            [hiiop.components.quests-browse :as p-b]
            [hiiop.components.activate :as p-a]
            [hiiop.components.register :as p-r]
            [hiiop.components.errors :as e]
            [hiiop.components.login :as p-l]
            [hiiop.components.part-party :refer [part-party]]
            [hiiop.config :refer [env google-maps-url]]
            [hiiop.routes.page-hierarchy :refer [hierarchy]]
            [hiiop.url :refer [redirect-to]]
            [hiiop.mangling :refer [parse-natural-number
                                    same-keys-with-nils]]
            [hiiop.schema :refer [Quest
                                  EditQuest
                                  NewQuest
                                  RegistrationInfo
                                  EditUser
                                  UserActivation
                                  QuestFilter
                                  QuestCategoryFilter
                                  NewPartyMember
                                  new-empty-quest
                                  new-empty-party-member
                                  new-empty-quest-filter
                                  new-empty-category-filter
                                  new-empty-registration-info
                                  new-empty-activation-info]]
            [hiiop.api-handlers :refer [get-quest
                                        get-secret-quest
                                        get-moderated-or-unmoderated-quest
                                        get-public-user
                                        get-private-user
                                        get-user-quests
                                        get-quest-party
                                        joinable-quest?
                                        get-moderated-quests
                                        get-unmoderated-quests
                                        get-party-member]]
            [hiiop.components.password-reset :refer [display-message
                                                     request-password-reset
                                                     password-reset]]
            [hiiop.db.core :as db]
            [hiiop.api-handlers :as api-handlers]))

(def autolink-url
  "//cdnjs.cloudflare.com/ajax/libs/autolinker/1.4.0/Autolinker.min.js")

(defn tr-from-req [req]
  (:tempura/tr req))

(defn create-context [req]
  {:tr (tr-from-req req)
   :config env
   :hierarchy hierarchy
   :identity (:identity req)
   :path-key (:handler (match-route hierarchy (:path-info req)))
   :current-locale (keyword (:current-locale req))})

(defn index [req]
  (let [context (create-context req)
        tr (:tr context)
        category-filter (atom (new-empty-category-filter))
        schema QuestCategoryFilter]
    (layout/render {:context context
                    :content (index-page/index-page {:context context
                                                     :schema schema
                                                     :category-filter category-filter})
                    :title (tr [:pages.index.title])
                    :metas [{:property "og:title"
                             :content (str (tr [:pages.index.social-title-prefix]) " " (tr [:pages.index.title]))}
                            {:property "og:description"
                             :content (tr [:pages.index.description])}
                            {:property "og:type"
                             :content "article"}
                            {:property "og:url"
                             :content (:site-base-url env)}
                            {:property "fb:app_id"
                             :content (get-in env [:social :facebook-app-id])}
                            {:property "og:image"
                             :content (str (:asset-base-url env) "/img/banner.jpg")}
                            {:name "twitter:creator"
                             :content (get-in env [:social :twitter-account])}
                            {:name "twitter:card"
                             :content (str (tr [:pages.index.social-title-prefix]) " " (tr [:pages.index.title]))}
                            {:name "twitter:description"
                             :content (tr [:pages.index.description])}]
                    :scripts ["//assets.juicer.io/embed.js"]})))

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
        tr (:tr context)
        user-id (get-in context [:identity :id])
        user-info (get-private-user {:id (sc/string->uuid user-id)
                                     :user-id (get-in req [:identity :id])})
        user-quests (get-user-quests {:user-id user-id})
        participating-quests (:attending user-quests)
        own-quests (:organizing user-quests)
        own-quest-ids (map :id own-quests)
        quests (into [] (concat own-quests
                                (filter #(not (some #{(:id %)} own-quest-ids))
                                        participating-quests)))]
    (layout/render {:title (str (tr [:pages.profile.title]) " " (:name user-info))
                    :context context
                    :content
                    (p-p/profile {:context context
                                  :user-info user-info
                                  :quests (atom quests)})})))

(defn edit-profile [req]
  (let [context (create-context req)
        tr (:tr context)
        user-id (get-in context [:identity :id])
        user-info (get-private-user {:id (sc/string->uuid user-id)
                                     :user-id (get-in req [:identity :id])})
        user-edit (atom {:name (:name user-info) :phone (:phone user-info)})
        errors (atom (same-keys-with-nils @user-edit))]
    (layout/render {:context context
                    :content (p-p/edit-profile {:context context
                                                :user-info user-info
                                                :user-edit user-edit
                                                :schema EditUser
                                                :errors errors})})))

(defn activate [req]
  (let [context (create-context req)
        tr (:tr context)
        token (try
                (schema.coerce/string->uuid
                 (get-in req [:params :token]))
                (catch Exception e
                  (log/error e)))
        token-info (api-handlers/validate-token token)]
    (layout/render
     {:title (tr [:pages.activate.title])
      :context context
      :no-script (not-empty (:errors token-info))
      :content
      (if (not (:errors token-info))
        (password-reset
         {:token-info token-info
          :token token
          :context context
          :api-fn nil})
        (display-message
         {:context context
          :title-key :errors.token.expired
          :message-key :errors.user.token.contact})
        )})))

(defn edit-quest-with-schema [{:keys [request schema quest party title-key]}]
  (let [context (create-context request)
        tr (:tr context)
        quest-atom (atom quest)
        party-atom (atom party)
        user (get-private-user {:id (:owner quest)
                                :user-id (get-in request [:identity :id])})
        errors (atom (same-keys-with-nils @quest-atom))]
    (layout/render {:title (tr [title-key])
                    :context context
                    :content
                    (quests/edit {:context context
                                  :quest quest-atom
                                  :party party-atom
                                  :user user
                                  :schema schema
                                  :errors errors})
                    :scripts
                    [google-maps-url
                     autolink-url]
                    })))

(defn browse-quests [req]
  (let [context (create-context req)
        tr (:tr context)
        quests (filter
                 :is-open
                 (get-moderated-quests))
        quest-filter (atom (new-empty-quest-filter))
        errors (atom (same-keys-with-nils @quest-filter))]
    (layout/render {:context context
                    :content
                    (p-b/list-quests {:quests quests
                                      :quest-filter quest-filter
                                      :context context
                                      :schema QuestFilter})
                    :title (tr [:actions.quest.browse])
                    :metas [{:property "og:title"
                             :content (tr [:action.quest.browse])}
                            {:property "og:content"
                             :content (tr [:pages.index.banner.content])}
                            {:property "og:type"
                             :content "article"}
                            {:property "og:url"
                             :content (str (:site-base-url env)
                                           (:url req))}
                            {:property "fb:app_id"
                             :content (get-in env [:social :facebook-app-id])}
                            {:property "og:image"
                             :content (str (:asset-base-url env) "/img/banner.jpg")}
                            {:name "twitter:creator"
                             :content (get-in env [:social :twitter-account])}
                            {:name "twitter:card"
                             :content (tr [:pages.index.banner.content])}]
                    :scripts
                    [google-maps-url]
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
        quest (get-moderated-or-unmoderated-quest
                {:id (parse-natural-number id)
                 :user-id (:id identity)})
        owner? (= (:owner quest) (:id identity))
        party (vec (get-quest-party
                    {:quest-id (:id quest)
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
        joinable (= true
                    (joinable-quest? {:quest-id (:id quest)}))
        owner-name (:name (get-public-user (:owner quest)))
        context (create-context req)
        tr (:tr context)]
    (if quest
      (layout/render {:title (:name quest)
                      :context context
                      :metas [{:property "og:title"
                               :content (:name quest)}
                              {:property "og:content"
                               :content (:description quest)}
                              {:property "og:type"
                               :content "article"}
                              {:property "og:url"
                               :content (str (:site-base-url env)
                                             (:uri req))}
                              {:property "fb:app_id"
                               :content (get-in env [:social :facebook-app-id])}
                              {:property "og:image"
                               :content (get-quest-image quest)}
                              {:name "twitter:creator"
                               :content (get-in env [:social :twitter-account])}
                              {:name "twitter:card"
                               :content (:description quest)}]
                      :content
                      (quest/quest {:context context
                                    :quest (atom (assoc quest
                                                        :owner-name owner-name))
                                    :url (str (:site-base-url env)
                                              (:uri env))
                                    :joinable joinable
                                    :empty-party-member empty-party-member
                                    :party-member-errors errors
                                    :party-member-schema NewPartyMember})
                      :scripts [autolink-url]}))))

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
        joinable (= true
                    (joinable-quest?
                      {:quest-id (:id quest)
                                      :secret-party secret-party}))
        owner-name (:name (get-public-user (:owner quest)))
        context (create-context req)
        tr (:tr context)]
    (if quest
      (layout/render {:title (:name quest)
                      :context context
                      :content
                      (quest/quest {:context context
                                    :joinable joinable
                                    :quest (atom (assoc quest
                                                        :owner-name owner-name))
                                    :empty-party-member empty-party-member
                                    :party-member-errors errors
                                    :party-member-schema NewPartyMember})}))))

(defn part-quest-party [req]
  (let [context (create-context req)
        tr (:tr context)
        quest-id (parse-natural-number
                  (get-in req [:params :quest-id]))
        quest (get-quest quest-id)
        member-id (get-in req [:params :member-id])
        party-member (get-party-member {:member-id (sc/string->uuid member-id)})]
    (if party-member
      (layout/render {:title (tr [:pages.quest.part.title])
                      :context context
                      :content
                      (part-party {:quest quest
                                   :party-member party-member
                                   :context context})})
      (redirect-to {:path-key :quest
                    :with-params [:quest-id quest-id]})
      )))

(defn moderate [req]
  (let [context (create-context req)
        tr (:tr context)
        id (:id (:identity context))
        user (get-private-user {:id (sc/string->uuid id)
                                :user-id id})
        is-moderator (:moderator user)
        unmoderated-quests (get-unmoderated-quests {:user-id id})]
    (if is-moderator
      (layout/render {:title (tr [:pages.moderate.title])
                      :context context
                      :content
                      (p-m/moderate-page
                       {:context context
                        :unmoderated-quests (atom unmoderated-quests)})
                      :scripts [autolink-url]})
      (redirect-to {:path-key :index}))))

(defn request-password-reset-page [req]
  (let [context (create-context req)
        tr (:tr context)]
    (layout/render {:title (tr [:pages.password-reset.title])
                    :context context
                    :content
                    (request-password-reset
                     {:context context})})
    ))

(defn password-reset-page [req]
  (let [context (create-context req)
        tr (:tr context)
        token (try
                (schema.coerce/string->uuid
                 (get-in req [:params :token]))
                (catch Exception e
                  (log/error e)))
        token-info (api-handlers/validate-token token)]
    (layout/render
     {:title (tr [:pages.password-reset.title])
      :context context
      :no-script (not-empty (:errors token-info))
      :content
      (if (not (:errors token-info))
        (password-reset
         {:token-info token-info
          :token token
          :context context
          :api-fn nil})
        (display-message
         {:context context
          :title-key :errors.token.expired
          :message-key :pages.password-reset.expired.text
          :link-to (path-for hierarchy :request-password-reset)})
        )})
    ))

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
   :edit-profile
   (authenticated edit-profile)
   :quest
   quest
   :secret-quest
   secret-quest
   :part-quest-party
   part-quest-party
   :browse-quests
   browse-quests
   :create-quest
   (authenticated create-quest)
   :edit-quest
   (authenticated edit-quest)
   :moderate
   (authenticated moderate)
   :request-password-reset
   request-password-reset-page
   :password-reset
   password-reset-page
   })

(def ring-handler
  (do
    (log/info hierarchy)
    (make-handler
     hierarchy
     (fn [handler]
       (log/info "handler" handler)
       (handler handlers)))))
