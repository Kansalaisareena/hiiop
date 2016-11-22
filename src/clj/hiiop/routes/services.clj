(ns hiiop.routes.services
  (:require [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [taoensso.timbre :refer [info]]
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
                  (let [lang-override (:lang-override req)]
                    (ok
                     (conj
                      (select-keys env [:dev :git-ref :langs])
                      {:accept-langs
                       (if lang-override
                         [lang-override]
                         (:tempura/accept-langs req))}))))
             
             (POST "/logout" []
                   :summary "Logs the user out."
                   api-handlers/logout)
             (POST "/login" []
                   :body-params [email :- Email, password :- Password]
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
                         (bad-request "User creation failed")))))
             (GET "/users/:id" []
                  :name ::user
                  :path-params [id :- s/Uuid]
                  :summary "Return user object"
                  api-handlers/get-user)
             (GET "/login-status" []
                  :return Boolean
                  api-handlers/login-status)
             (GET "/show-session" [] api-handlers/show-session))))
  
