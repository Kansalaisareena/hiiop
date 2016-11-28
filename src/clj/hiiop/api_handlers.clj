(ns hiiop.api-handlers
  (:require [ring.util.http-response :refer :all]
            [mount.core :as mount]
            [buddy.auth :refer [authenticated?]]
            [buddy.hashers :as hashers]
            [compojure.api.sweet :as s]
            [schema.coerce :as coerce]
            [taoensso.timbre :as log]
            [hiiop.db.core :as db]
            [hiiop.time :as time]))

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

(defn time-from [keyword map]
  (time/from-string (keyword map)))

(defn quest-to-db-form [quest-from-api user]
  (let [distinct-hashtags (vec (distinct (:hashtags quest-from-api)))
        distinct-categories (vec (distinct (:categories quest-from-api)))
        is-open (if (:is-open quest-from-api)
                  (:is-open quest-from-api)
                  true)
        start-time (time-from :start-time quest-from-api)
        end-time (time-from :end-time quest-from-api)
        picture-id (:picture-id quest-from-api)
        with-added-fields (assoc quest-from-api
                                 :start-time start-time
                                 :end-time end-time
                                 :hashtags distinct-hashtags
                                 :picture picture-id
                                 :is-open is-open
                                 :owner (:id user)
)]
    (dissoc with-added-fields
            :picture-id)
    ))

(defn time-to-string [keyword map]
  (time/to-string (keyword map)))

(defn start-and-end-time-to-string [map]
  (log/info "start-and-end-time-to-string" map)
  (assoc map
         :start-time (time-to-string :start-time map)
         :end-time   (time-to-string :end-time map)))

(defn add-quest [{:keys [quest user]}]
  (let [quest-to-db (quest-to-db-form quest user)
        create-with! (if (:is-open quest-to-db)
                       db/add-unmoderated-open-quest!
                       db/add-unmoderated-secret-quest!)
        quest-id (:id (create-with! (db/->snake_case_keywords quest-to-db)))
        quest-from-db (db/get-quest-by-id {:id quest-id})
        quest-to-api (start-and-end-time-to-string quest-from-db)]
    quest-to-api))

(defn get-quest [{{id :id} :params}]
  (ok (db/get-quest-by-id {:id id})))

(defn join-quest [params]
  (created ""))
