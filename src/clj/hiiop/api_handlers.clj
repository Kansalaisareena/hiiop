(ns hiiop.api-handlers
  (:require [ring.util.http-response :refer :all]
            [hiiop.users :as users]
            [mount.core :as mount]
            [buddy.auth :refer [authenticated?]]))

(defn login-status
  [request]
  (ok (str (:identity request))))

(defn show-session
  [request] 
  (ok (str request " " (authenticated? request) )))

(defn logout
  [{session :session}]
  (assoc (ok) :session (dissoc session :identity)))

(defn login-handler
  [{session :session :as request}]
  (let [email (get-in request [:query-params "email"])
        password (get-in request [:query-params "password"])
        password-ok (users/check-password email password)
        user-id (users/get-user-id email)]
    (if password-ok
      (assoc (ok (str user-id))
             :session (assoc session :identity user-id))
      (ok "bad password/username combo"))))

