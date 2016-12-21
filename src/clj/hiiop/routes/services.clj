(ns hiiop.routes.services
  (:require [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [ring.swagger.upload :refer [wrap-multipart-params TempFileUpload]]
            [cheshire.core :refer [generate-string parse-string]]
            [buddy.auth.middleware :refer [wrap-authentication]]
            [schema.core :as s]
            [taoensso.timbre :as log]
            [hiiop.middleware :refer [api-authenticated]]
            [hiiop.time :as time]
            [hiiop.config :refer [env]]
            [hiiop.api-handlers :as api-handlers]
            [hiiop.schema :refer :all]
            [hiiop.db.core :as db]
            [schema.coerce :as sc]))

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
        api-handlers/contentful-hook)

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
          :body-params [email :- Email name :- s/Str]
          :summary "Create a new user and email password token"
          (fn [request]
            (-> (api-handlers/register
                 {:email email
                  :name name
                  :locale (:current-locale request)})
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
              (bad-request))
            ))
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
                    (created (path-for ::picture {:id (str (:id %1))}) %1)
                    (bad-request %1))))))

        (GET "/:id" []
          :name        ::picture
          :path-params [id :- String]
          :summary     "Get picture"
          :return      Picture
          (fn [request]
            (-> (api-handlers/get-picture id)
                (#(if %1
                    (ok %1)
                    (not-found)))))))

      (context "/quests" []
        :tags ["quest"]

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

        (PUT "/:id" []
          :name        ::quest-edit
          :path-params [id :- Long]
          :body        [quest Quest]
          :summary     "Edit quest"
          :return      Quest
          (fn [request]
            (-> (api-handlers/edit-quest
                 {:quest quest
                  :user (:identity request)})
                (#(if (not (:errors %1))
                    (ok %1)
                    (bad-request %1))))
            ))

        ;; (POST "/:id/join" []
        ;;  :name ::quest-join
        ;;  :path-params [id :- s/Int]
        ;;  :body [NewPartyMember]
        ;;  :summary "Join a quest"
        ;;  api-handlers/join-quest)
        ))))
