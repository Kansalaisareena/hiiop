(ns hiiop.client-api
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]))

(defn login [{:keys [user pass] :as credentials}]
  (go
    (let [response (<! (http/post "/api/v1/login" {:json-params credentials}))]
    (= (:status response) 200))))
