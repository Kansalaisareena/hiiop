(ns hiiop.routes.services
  (:require [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [hiiop.config :refer [env]]
            [hiiop.api-handlers :as api-handlers]))

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
        (ok (select-keys env [:dev :lang :git-ref])))

      (POST "/login" []
        :query-params [email :- String, password :- String]
        :summary "Tries to log the user in. Returns
                 true/false on success/failure"
        api-handlers/login-handler)

      (GET "/login-status" [] api-handlers/login-status)

      (GET "/show-session" [] api-handlers/show-session)

      (GET "/logout" []
        :summary "Logs the user out."
        api-handlers/logout))))
