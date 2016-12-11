(ns hiiop.routes.services
  (:require [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [taoensso.timbre :as log]
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


      (context "/users" []
               :tags ["user"]
               (GET "/users/:id" []
                    :name ::user
                    :path-params [id :- s/Uuid]
                    :return User
                    :summary "Return user object"
                    api-handlers/get-user)

               (POST "/register" []
                     :body-params [email :- Email]
                     :summary "Create a new user and email password token"
                     (fn [request]
                       (let [id (api-handlers/register request)]
                         (if id
                           (created (path-for ::user {:id (str id)}))
                           (bad-request {:error "User registration failed"})))))

               (POST "/activate" []
                     :body [activation UserActivation]
                     :summary "Activates inactive user"
                     api-handlers/activate))

      (context "/quests" []
        :tags ["quest"]

        (POST "/add" []
          :name ::add-quest
          :body [new-quest NewQuest]
          :summary "Create a new quest"
          :return Quest
          (fn [request]
            (let [quest (api-handlers/add-quest
                         {:quest new-quest
                          :user (:identity request)})]
              (if quest
                (created (path-for ::quest {:id (:id quest)}) quest)
                (bad-request {:error "Failed to add quest!"})))))

        (GET "/:id" []
          :name ::quest
          :path-params [id :- s/Int]
          :summary "Get quest"
          :return Quest
          api-handlers/get-quest)

        ;; (POST "/:id/join" []
        ;;  :name ::quest-join
        ;;  :path-params [id :- s/Int]
        ;;  :body [NewPartyMember]
        ;;  :summary "Join a quest"
        ;;  api-handlers/join-quest)
        )
      )))
