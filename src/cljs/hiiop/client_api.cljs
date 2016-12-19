(ns hiiop.client-api
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [schema.core :as s])
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [taoensso.timbre :as log]))

(defn login [{:keys [user pass] :as credentials}]
  (go
    (let [response (<! (http/post "/api/v1/login"
                                  {:json-params credentials}))]
      (= (:status response) 200))))

(defn register [credentials]
  (go
    (let [response (<! (http/post
                        "/api/v1/users/register"
                        {:json-params credentials}))
          status (:status response)
          body (:body response)]
      (= status 201))))

(defn validate-token [token]
  (go
    (let [response (<! (http/post
                        "api/v1/users/validate-token"
                        {:json-params token}))
          status (:status response)
          body (:body response)]
      body)))

(defn add-quest [quest]
  (log/info "add-quest called with" quest)
  (go
    (let [response (<! (http/post "/api/v1/quests/add"
                                  {:json-params quest}))
          status (:status response)
          body (:body response)]
      body)))

(defn edit-quest [quest])
