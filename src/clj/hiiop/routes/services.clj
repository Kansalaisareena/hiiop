(ns hiiop.routes.services
  (:require [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [ring.swagger.upload :refer [wrap-multipart-params TempFileUpload]]
            [cheshire.core :refer [generate-string parse-string]]
            [schema.core :as s]
            [taoensso.timbre :as log]
            [schema.coerce :as sc]
            [hiiop.contentful :as cf]
            [hiiop.middleware :refer [api-authenticated wrap-simple-auth]]
            [hiiop.time :as time]
            [hiiop.config :refer [env]]
            [hiiop.api-handlers :as api-handlers]
            [hiiop.schema :refer :all]
            [hiiop.db.core :as db]))

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

      (GET "/config" req []
           (let [current-locale (:current-locale req)]
             (ok
              (conj
               (select-keys env [:time-zone :dev :git-ref :langs])
               {:accept-langs (:tempura/accept-langs req)
                :now (time/now-utc)
                :current-locale (keyword current-locale)
                :identity (:identity req)}))))

      (POST "/logout" []
        :summary "Logs the user out."
        api-handlers/logout)

      (POST "/login" []
        :body [credentials UserCredentials]
        :summary "Tries to log the user in. Returns
                 true/false on success/failure"
        api-handlers/login)

      (POST "/contentful-hook" []
        :body [cfobject CfObject]
        :summary "Handles contentful webhook."
        (wrap-simple-auth {:username (get-in env [:contentful :webhook-user])
                           :password (get-in env [:contentful :webhook-password])}
                          (fn [request]
                            (try (do (cf/process-item cfobject)
                                     (ok))
                                 (catch Exception e
                                   (log/info "Contentful hook failed: " e)
                                   (internal-server-error))))))

      (context "/users" []
        :tags ["user"]
        (GET "/:id" []
          :name ::user
          :path-params [id :- s/Uuid]
          :summary "Return user object"
          (fn [request]
            (-> (api-handlers/get-user (:id (:params request)))
                (#(if (:errors %1)
                    (bad-request %1)
                    (ok %1))))))

        (POST "/register" []
          :body [registration RegistrationInfo]
          :summary "Create a new user and email password token"
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
          (fn [request]
            (-> (api-handlers/validate-token token)
                (#(if (:errors %1)
                    (bad-request %1)
                    (ok %1))))))

        (POST "/activate" []
          :body [activation UserActivation]
          :summary "Activates inactive user"
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
                  (-> (api-handlers/edit-user {:new-user new-user :id id})
                      (#(if (not (:errors %1))
                          (ok %1)
                          (bad-request %1))))
                  (unauthorized))))

        (POST "/reset-password" []
              :body [email Email]
              :summary "Creates a password reset token and sends it to
              the given email address."
              (fn [request]
                (try (api-handlers/reset-password email)
                     (catch Exception e
                       (log/info e)))
                (ok)))

        (POST "/change-password" []
          :body [password-reset UserActivation]
          :summary "Activates inactive user"
          (fn [request]
            (if (api-handlers/change-password password-reset)
             (ok)
             (bad-request {:error "Password change failed."})))))

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
          :summary     "Get picture"
          :return      Picture
          (fn [request]
            (-> (api-handlers/get-picture id)
                (#(if %1
                    (ok %1)
                    (not-found)))))))

      (context "/quests" []
        :tags ["quest"]

        (GET "/own" []
             :name ::get-own-quests
             :middleware [api-authenticated]
             :return [Quest]
             (fn [request]
               (let [owner (:id (:identity request))
                     quests (api-handlers/get-quests-for-owner owner)]
                 (if (nil? (:errors quests))
                   (ok quests)
                   (bad-request quests)))))

        (GET
         "/moderated" []
         :name ::get-moderated-quests
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
          :summary     "Get quest"
          :return      Quest
          (fn [request]
            (let [quest (api-handlers/get-quest id)]
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
                           :user (:identity request)})]
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
                      (bad-request %1)))))
            ))

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
                       (unauthorized))))
               ))

        (POST "/:quest-id/moderate-reject" []
             :name        ::quest-moderate-reject
             :path-params [quest-id :- Long]
             :body        [moderation Moderation]
             :summary     "Reject quest"
             :middleware  [api-authenticated]
             :return      Quest
             (fn [request]
               (-> (api-handlers/moderate-reject-quest
                    {:quest-id quest-id
                     :user-id (get-in request [:identity :id])
                     :message (:message moderation)})
                   (#(if (not (:errors %1))
                       (ok %1)
                       (unauthorized))))
               ))

        (POST "/:quest-id/party" []
          :name        ::quest-join
          :path-params [quest-id :- Long]
          :body        [new-member NewPartyMember]
          :summary     "Join a quest"
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
                    (bad-request %1))))
            ))

        (GET "/:id/secret/:secret-party" []
          :name        ::secret-quest
          :path-params [id :- Long
                        secret-party :- s/Uuid]
          :summary     "Get secret quest"
          :return      Quest
          (fn [request]
            (let [quest (api-handlers/get-secret-quest
                         {:id id
                          :secret-party secret-party})]
              (if quest
                (ok quest)
                (not-found)))))

        (GET "/:quest-id/party" []
          :name        ::quest-party
          :path-params [quest-id :- Long]
          :middleware  [api-authenticated]
          :summary     "Get quest party"
          (fn [request]
            (-> (api-handlers/get-quest-party
                 {:quest-id quest-id
                  :user (:identity request)})
                (#(if (not (:errors %1))
                    (ok %1)
                    (bad-request %1))))
            ))

        (GET "/:quest-id/party/:member-id" []
          :name        ::quest-party-member
          :path-params [quest-id :- Long
                        member-id :- s/Uuid]
          :return      PartyMember
          :summary     "Get party member info"
          (fn [request]
            (-> (api-handlers/get-party-member {:member-id member-id})
                (#(if %1
                    (ok %1)
                    (bad-request {:errors {:quest "Not found"}}))))
            ))

        (DELETE "/:quest-id/party/:member-id" []
          :name        ::quest-delete-party-member
          :path-params [quest-id :- Long
                        member-id :- s/Uuid]
          :summary     "Delete party member from party"
          (fn [request]
            (log/info "delete member" member-id)
            (-> (api-handlers/remove-party-member
                 {:member-id member-id})
                (#(if %1
                    (no-content)
                    (bad-request
                     {:errors {:party :errors.quest.party.member.remove.failed}}))))
            ))

        ))))
