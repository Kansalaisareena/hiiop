(ns hiiop.client-api
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [schema.core :as s])
  (:require [cljs-http.client :as http]
            [schema.coerce :as sc]
            [cljs.core.async :refer [<!]]
            [taoensso.timbre :as log]))

(def base-path "/api/v1")

(defn login [{:keys [user pass] :as credentials}]
  (go
    (let [response (<! (http/post (str base-path "/login")
                                  {:json-params credentials}))]
      (= (:status response) 200))))

(defn logout []
  (go
    (<! (http/post (str base-path "/logout")))))

(defn register [credentials]
  (go
    (let [response (<! (http/post
                        (str base-path "/users/register")
                        {:json-params credentials}))
          status (:status response)
          body (:body response)]
      (conj {:success (= status 200)}
            body))))

(defn validate-token [token]
  (go
    (let [response (<! (http/post
                        (str base-path "/users/validate-token")
                        {:json-params token}))
          status (:status response)
          body (:body response)]
      body)))

(defn activate-user [activation-info]
  (go
    (let [response (<! (http/post
                        (str base-path "/users/activate")
                        {:json-params activation-info}))
          status (:status response)
          body (:body response)]
      body)))

(defn get-quest [id]
  (go
    (let [response (<! (http/get (str base-path "/quests/" id)))]
      (when (= (:status response) 200)
        (:body response)))))

(defn get-secret-quest [{:keys [id secret-party]}]
  (go
    (let [response (<! (http/get
                        (str base-path "/quests/" id "/secret/" secret-party)))]
      (when (= (:status response) 200)
        (:body response)))))

(defn get-moderated-quests []
  (go
    (let [response (<! (http/get (str base-path "/quests/moderated")))]
      (when (= (:status response) 200)
        (:body response)))))

(defn get-unmoderated-quests []
  (go
    (let [response (<! (http/get (str base-path "/quests/unmoderated")))]
      (when (= (:status response) 200)
        (:body response)))))


(defn get-own-quests []
  (go
    (let [response (<! (http/get (str base-path "/quests/own")))]
      (when (= (:status response) 200)
        (:body response)))))

(defn add-quest [quest]
  (log/info "add-quest called with" quest)
  (go
    (let [response (<! (http/post (str base-path "/quests/add")
                                  {:json-params quest}))
          status (:status response)
          body (:body response)]
      {:success (= status 201)
       :body body})))

(defn delete-quest [id]
  (log/info "delete-quest called with" id)
  (go
    (let [response (<! (http/delete
                        (str base-path "/quests/" id)))]
      (= (:status response) 204))))

(defn edit-quest [quest]
  (log/info "edit-quest called with" quest)
  (go
    (let [response (<! (http/put (str base-path "/quests/" (:id quest))
                                  {:json-params quest}))
          status (:status response)
          body (:body response)]
      {:success (= status 200)
       :body body})))

(defn get-quest-party [id]
  (log/info "get-quest-party" id)
  (go
    (let [response (<! (http/get
                        (str base-path "/quests/" id "/party")))
          status (:status response)
          body (:body response)]
      (when (= status 200)
        body))))

(defn join-quest [quest-id party-member]
  (log/info "join-quest called with" quest-id party-member)
  (go
    (let [response (<! (http/post
                        (str base-path "/quests/" quest-id "/party")
                        {:json-params party-member}))
          status (:status response)
          body (:body response)]
      {:success (= status 201)
       :body body})))

(defn remove-party-member [{:keys [quest-id member-id]}]
  (log/info "remove-party-member called with" quest-id member-id)
  (go
    (let [response (<! (http/delete
                        (str base-path
                             "/quests/" quest-id
                             "/party/" member-id)))]
      (log/info "status" (= (:status response) 204))
      (when (= (:status response) 204)
        true))))

(defn get-user-info [id]
  (go
    (let [response (<! (http/get
                        (str "/api/v1/users/" id)))
          status (:status response)
          body (:body response)]
      (when (= status 200)
        body))))

(defn get-party-member [{:keys [quest-id member-id]}]
  (go
    (let [response (<! (http/get
                        (str base-path
                             "/quests/" quest-id
                             "/party/" member-id)
                        ))
          status (:status response)
          body (:body response)]
      (when (= status 200)
        body))))

(defn moderate-quest [quest-id]
  (go
    (let [response (<! (http/post (str base-path
                                       "/quests/" quest-id
                                       "/moderate-accept")))
          status (:status response)
          body (:body response)]
      (when (= status 200)
        body))))

(defn reject-quest [quest-id message]
  (go
    (let [response (<! (http/post (str base-path
                                       "/quests/" quest-id
                                       "/moderate-reject")
                                  {:json-params message}))
          status (:status response)
          body (:body response)]
      (when (= status 200)
        body))))
