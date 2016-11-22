(ns hiiop.api-handlers
  (:require [ring.util.http-response :refer :all]
            [mount.core :as mount]
            [buddy.auth :refer [authenticated?]]
            [buddy.hashers :as hashers]
            [hiiop.db.core :as db]
            [compojure.api.sweet :as s]
            [schema.coerce :as coerce]))

(defn login-status
  [request]
  (ok (authenticated? request)))

(defn show-session
  [request] 
  (ok (str request)))

(defn logout
  [{session :session}]
  (assoc (ok) :session (dissoc session :identity)))

(defn login
  [{session :session
    {{:keys [email password]} :credentials} :body-params}]
  (let [password-ok (db/check-password email password)
        user-id (db/get-user-id {:email email})]
    (if password-ok
      (assoc (ok)
             :session (assoc session :identity user-id))
      (unauthorized))))

(defn register
  [{{{:keys [password name email]} :user} :body-params :as body-params}]
  (let [hash (hashers/derive password {:alg :bcrypt+blake2b-512})
        response (db/add-full-user! {:email email
                                     :name name
                                     :pass hash})]
    (:id response)))

(defn get-user [{{id :id} :params}]
  (ok (str (db/get-user-by-id {:id (coerce/string->uuid id)}))))

