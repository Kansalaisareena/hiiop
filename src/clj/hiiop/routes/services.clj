(ns hiiop.routes.services
  (:require [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [taoensso.timbre :refer [info]]
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
                :current-locale (keyword current-locale)}))))

      (POST "/logout" []
        :summary "Logs the user out."
        api-handlers/logout)

      (POST "/login" []
        :body-params [credentials :- UserCredentials]
        :summary "Tries to log the user in. Returns
                 true/false on success/failure"
        api-handlers/login)

      (POST "/register" []
        :body-params [user :- UserRegistration]
        :summary "Create a new full user"
        (fn [request]
          (let [id (api-handlers/register request)]
            (if id
              (created (path-for ::user {:id (str id)}))
              (bad-request {:error "User registration failed"})))))

      (GET "/users/:id" []
        :name ::user
        :path-params [id :- s/Uuid]
        :summary "Return user object"
        api-handlers/get-user)

      (GET "/login-status" []
        :return Boolean
        api-handlers/login-status)

      (GET "/show-session" [] api-handlers/show-session)

      (context "/quest" []
        :tags ["quest"]

        (POST "/add" []
          :body [new-quest NewQuest]
          :summary "Create a new quest"
          :return Quest
          (fn [request]
            (let [quest (api-handlers/add-quest
                         {:quest new-quest
                          :user {:id nil}})]
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
      ))))
