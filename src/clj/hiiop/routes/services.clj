(ns hiiop.routes.services
  (:require [taoensso.timbre :as log]
            [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [ring.swagger.upload :refer [wrap-multipart-params TempFileUpload]]
            [cheshire.core :refer [generate-string parse-string]]
            [schema.core :as s]
            [hiiop.contentful :as cf]
            [hiiop.middleware :refer [api-authenticated
                                      wrap-simple-auth
                                      wrap-private-cache-headers]]
            [hiiop.time :as time]
            [hiiop.config :refer [env]]
            [hiiop.api-handlers :as api-handlers]
            [hiiop.schema :refer :all]))

(defapi service-routes
  {:swagger {:ui "/--help"
             :spec "/swagger.json"
             :data {:info {:version "1.0.0"
                           :title "Sample API"
                           :description "Sample Services"}}}}

  (context "/api" []
    :tags ["API"]

    (context "/v1" []
      :tags ["V1"]

      (GET "/config" []
        :middleware  [wrap-private-cache-headers]
        (fn [req]
          (let [current-locale (:current-locale req)]
            (ok
             (conj
              (select-keys env [:time-zone
                                :dev
                                :git-ref
                                :langs
                                :hiiop-blog-base-url
                                :site-base-url
                                :js-logs])
              {:accept-langs (:tempura/accept-langs req)
               :now (time/now-utc)
               :current-locale (keyword current-locale)
               :identity (:identity req)})))))

      (POST "/logout" []
        :summary "Logs the user out."
        :middleware  [wrap-private-cache-headers]
        api-handlers/logout)

      (POST "/login" []
        :body [credentials UserCredentials]
        :summary "Tries to log the user in. Returns
                 true/false on success/failure"
        :middleware  [wrap-private-cache-headers]
        api-handlers/login)

      (POST "/contentful-hook" []
        :body [cfobject CfObject]
        :summary "Handles contentful webhook."
        :middleware  [wrap-private-cache-headers]
        (wrap-simple-auth
         {:username (get-in env [:contentful :webhook-user])
          :password (get-in env [:contentful :webhook-password])}
         (fn [request]
           (try (do (cf/update-all-items)
                    (ok))
                (catch Exception e
                  (log/info "Contentful hook failed: " e)
                  (internal-server-error))))))

      (context "/users" []
        :tags ["user"]

        (GET "/:id" []
          :name ::public-user
          :path-params [id :- s/Uuid]
          :summary "Return user object"
          :middleware  [wrap-private-cache-headers]
          (fn [request]
            (-> (api-handlers/get-public-user id)
                (#(if (:errors %1)
                    (bad-request %1)
                    (ok %1))))))

        (GET "/private/:id" []
          :name ::user
          :path-params [id :- s/Uuid]
          :middleware  [api-authenticated]
          :summary "Return user object"
          (fn [request]
            (-> (api-handlers/get-private-user {:id id
                                                :user-id (get-in request [:identity :id])})
                (#(if (:errors %1)
                    (bad-request %1)
                    (ok %1))))))

        (POST "/register" []
          :body [registration RegistrationInfo]
          :summary "Create a new user and email password token"
          :middleware  [wrap-private-cache-headers]
          (fn [request]
            (-> (api-handlers/register
                 (assoc registration
                        :locale (:current-locale request)))
                (#(if (:errors %1)
                    (bad-request %1)
                    (ok))))))

        (POST "/validate-token" []
          :summary "Verify if a token is valid and returns its expiry date and user email"
          :body-params [token :- s/Uuid]
          :middleware  [wrap-private-cache-headers]
          (fn [request]
            (-> (api-handlers/validate-token token)
                (#(if (:errors %1)
                    (bad-request %1)
                    (ok %1))))))

        (POST "/activate" []
          :body [activation TokenAndPassword]
          :summary "Activates inactive user"
          :middleware  [wrap-private-cache-headers]
          (fn [request]
            (if (api-handlers/activate activation)
              (ok)
              (bad-request))))

        (PUT  "/:id" []
          :name        ::edit-user
          :path-params [id :- s/Uuid]
          :body        [new-user EditUser]
          :middleware  [api-authenticated]
          :summary     "Updates a user"
          (fn [request]
            (if (= id (:id (:identity request)))
              (-> (api-handlers/edit-user {:new-user new-user 
                                           :id id 
                                           :request-user-id (get-in request [:identity :id])
                                           :locale (:current-locale request)})
                  (#(if (not (:errors %1))
                      (ok %1)
                      (bad-request %1))))
              (unauthorized))))

        (POST "/reset-password" []
          :body [email Email]
          :middleware  [wrap-private-cache-headers]
          :summary "Creates a password reset token and sends it to
              the given email address."
          (fn [request]
            (try (api-handlers/reset-password email)
                 (catch Exception e
                   (log/info e)))
            (ok)))

        (POST "/change-password" []
          :middleware  [wrap-private-cache-headers]
          :body [password-reset TokenAndPassword]
          :summary "Change password"
          (fn [request]
            (-> (api-handlers/change-password password-reset)
                (#(if (not (:errors %1))
                    (ok)
                    (bad-request %1))))))
        )

      (context "/pictures" []
        :tags ["picture"]

        (POST "/add" []
          :name             ::add-picture
          :multipart-params [file :- TempFileUpload]
          :middleware       [wrap-multipart-params api-authenticated]
          :summary          "Handles picture upload"
          :return           Picture
          (fn [request]
            (-> (api-handlers/add-picture
                 {:file file
                  :user (:identity request)})
                (#(if (not (:errors %1))
                    (created
                     (path-for ::picture {:id (str (:id %1))})
                     %1)
                    (bad-request
                     %1))))))

        (GET "/:id" []
          :name        ::picture
          :path-params [id :- s/Uuid]
          :middleware  [wrap-private-cache-headers]
          :summary     "Get picture"
          :return      Picture
          (fn [request]
            (-> (api-handlers/get-picture id)
                (#(if %1
                    (ok %1)
                    (not-found)))))))

      (context "/quests" []
        :tags ["quest"]

        (GET "/user" []
          :name ::get-user-quests
          :middleware [api-authenticated]
          :summary "Get all users quests"
          :return UserQuests
          (fn [request]
            (let [user-id (get-in request [:identity :id])
                  quests (api-handlers/get-user-quests
                          {:user-id user-id})]
              (if (nil? (:errors quests))
                (ok quests)
                (bad-request quests)))))

        (GET "/moderated" []
          :name ::get-moderated-quests
          :middleware  [wrap-private-cache-headers]
          :summary "Get all moderated quests"
          :return [Quest]
          (fn [quest]
            (let [quests (api-handlers/get-moderated-quests)]
              (if (nil? (:errors quests))
                (ok quests)
                (bad-request quests)))))

        (GET "/unmoderated" []
          :name ::get-unmoderated-quests
          :middleware [api-authenticated]
          :summary "Get all unmoderated quests"
          :return [Quest]
          (fn [request]
            (let [quests (api-handlers/get-unmoderated-quests
                          {:user-id (get-in request [:identity :id])})]
              (if (nil? (:errors quests))
                (ok quests)
                (bad-request quests)))))

        (POST "/add" []
          :name       ::add-quest
          :body       [new-quest NewQuest]
          :middleware [api-authenticated]
          :summary    "Create a new quest"
          :return     Quest
          (fn [request]
            (let [quest (api-handlers/add-quest
                         {:quest new-quest
                          :user (:identity request)})]
              (if quest
                (created (path-for ::quest {:id (:id quest)}) quest)
                (bad-request {:error "Failed to add quest!"})))))

        (GET "/:id" []
          :name        ::quest
          :path-params [id :- Long]
          :middleware  [wrap-private-cache-headers]
          :summary     "Get quest"
          :return      Quest
          (fn [request]
            (let [quest (api-handlers/get-quest id)]
              (if quest
                (ok quest)
                (not-found)))))

        (GET "/moderated-or-unmoderated/:id" []
          :name ::moderated-or-unmoderated-quest
          :path-params [id :- Long]
          :middleware  [api-authenticated]
          :summary "Get moderated or unmoderated quest to be used for quest edit page."
          :return Quest
          (fn [request]
            (let [quest (api-handlers/get-moderated-or-unmoderated-quest
                         {:id id
                          :user-id (get-in request [:identity :id])})]
              (if quest
                (ok quest)
                (not-found)))))

        (GET "/moderated/:id" []
          :name        ::moderated-quest
          :path-params [id :- Long]
          :middleware  [api-authenticated]
          :summary     "Get quest"
          :return      Quest
          (fn [request]
            (let [quest (api-handlers/get-quest id (:id request))]
              (if quest
                (ok quest)
                (unauthorized)))))

        (DELETE "/:id" []
          :name        ::quest-delete
          :path-params [id :- Long]
          :middleware  [api-authenticated]
          :summary     "Delete quest"
          (fn [request]
            (let [result (api-handlers/delete-quest
                          {:id id
                           :user-id (get-in request [:identity :id])})]
              (if (nil? (:errors result))
                (no-content)
                (bad-request result)))))

        (PUT "/:id" []
          :name        ::quest-edit
          :path-params [id :- Long]
          :body        [quest EditQuest]
          :middleware  [api-authenticated]
          :summary     "Edit quest"
          :return      Quest
          (fn [request]
            (-> (api-handlers/edit-quest
                 {:quest quest
                  :user (:identity request)})
                (#(if (not (:errors %1))
                    (ok %1)
                    (cond
                      (get-in %1 [:errors :unauthorized])
                      (unauthorized %1)
                      :else
                      (bad-request %1)))))))

        (POST "/:quest-id/moderate-accept" []
          :name        ::quest-moderate-accept
          :path-params [quest-id :- Long]
          :summary     "Accept quest"
          :middleware  [api-authenticated]
          :return      Quest
          (fn [request]
            (-> (api-handlers/moderate-accept-quest {:quest-id quest-id
                                                     :user-id (get-in request [:identity :id])})
                (#(if (not (:errors %1))
                    (ok %1)
                    (unauthorized))))))

        (POST "/:quest-id/moderate-reject" []
          :name        ::quest-moderate-reject
          :path-params [quest-id :- Long]
          :body        [moderation Moderation]
          :summary     "Reject quest"
          :middleware  [api-authenticated]
          (fn [request]
            (-> (api-handlers/moderate-reject-quest
                 {:quest-id quest-id
                  :user-id (get-in request [:identity :id])
                  :message (:message moderation)})
                (#(if (not (:errors %1))
                    (ok %1)
                    (unauthorized))))))

        (POST "/:quest-id/party" []
          :name        ::quest-join
          :path-params [quest-id :- Long]
          :body        [new-member NewPartyMember]
          :summary     "Join a quest"
          :middleware  [wrap-private-cache-headers]
          (fn [request]
            (-> (api-handlers/join-quest
                 {:id quest-id
                  :new-member new-member
                  :user (:identity request)
                  :locale (:current-locale request)}
                 )
                (#(if (not (:errors %1))
                    (created
                     (path-for ::quest-party-member
                               {:quest-id quest-id
                                :member-id (str (:id %1))})
                     %1)
                    (bad-request %1))))))

        (GET "/:quest-id/joinable" []
          :name ::joinable-quest?
          :path-params [quest-id :- Long]
          :summary "Check if a quest is joinable or not"
          :middleware  [wrap-private-cache-headers]
          :return s/Bool
          (fn [request]
            (-> (api-handlers/joinable-quest?
                 {:quest-id quest-id})
                (#(if (not (:errors %1))
                    (ok %1)
                    (bad-request %1))))))

        (GET "/:id/secret/:secret-party" []
          :name        ::secret-quest
          :path-params [id :- Long
                        secret-party :- s/Uuid]
          :summary     "Get secret quest"
          :middleware  [wrap-private-cache-headers]
          :return      Quest
          (fn [request]
            (let [quest (api-handlers/get-secret-quest
                         {:id id
                          :secret-party secret-party})]
              (if quest
                (ok quest)
                (not-found)))))

        (GET "/:id/secret/:secret-party/joinable" []
          :name ::joinable-secret-quest?
          :path-params [id :- Long
                        secret-party :- s/Uuid]
          :summary "Check if a secret quest is joinable or not"
          :middleware  [wrap-private-cache-headers]
          :return s/Bool
          (fn [request]
            (-> (api-handlers/joinable-quest?
                 {:quest-id id
                  :secret-party secret-party})
                (#(if (not (:errors %1))
                    (ok %1)
                    (bad-request %1))))))

        (GET "/:quest-id/get-member-info" []
          :name        ::quest-party-member-info
          :path-params [quest-id :- Long]
          :summary     "Get party member info for current user"
          :middleware  [wrap-private-cache-headers]
          :return      PartyMember
          (fn [request]
            (-> (api-handlers/get-party-member-info-for-user {:quest-id quest-id
                                                              :user-id (get-in request [:identity :id])})
                (#(if %1
                    (ok %1)
                    (bad-request {:errors {:user-id "Not in quest"}}))))))

        (GET "/:quest-id/party" []
          :name        ::quest-party
          :path-params [quest-id :- Long]
          :summary     "Get quest party"
          :middleware  [api-authenticated]
          (fn [request]
            (-> (api-handlers/get-quest-party
                 {:quest-id quest-id
                  :user (:identity request)})
                (#(if (not (:errors %1))
                    (ok %1)
                    (bad-request %1))))))

        (GET "/:quest-id/party/:member-id" []
          :name        ::quest-party-member
          :path-params [quest-id :- Long
                        member-id :- s/Uuid]
          :summary     "Get party member info"
          :middleware  [wrap-private-cache-headers]
          :return      PartyMember
          (fn [request]
            (-> (api-handlers/get-party-member {:member-id member-id})
                (#(if %1
                    (ok %1)
                    (bad-request {:errors {:quest "Not found"}}))))))

        (DELETE "/:quest-id/party/:member-id" []
          :name        ::quest-delete-party-member
          :path-params [quest-id :- Long
                        member-id :- s/Uuid]
          :summary     "Delete party member from party"
          :middleware  [wrap-private-cache-headers]
          (fn [request]
            (log/info "delete member" member-id)
            (-> (api-handlers/remove-party-member
                 {:member-id member-id})
                (#(if %1
                    (no-content)
                    (bad-request
                     {:errors {:party :errors.quest.party.member.remove.failed}}))))
            ))))))
