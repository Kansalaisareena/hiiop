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


(defn add-quest [quest]
  (log/info "add-quest called with" quest)
  (go
    (let [response (<! (http/post (str base-path "/quests/add")
                                  {:json-params quest}))
          status (:status response)
          body (:body response)]
      body)))

(defn edit-quest [quest])
