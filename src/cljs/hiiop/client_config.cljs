(ns hiiop.client-config
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<!]]
            [cljs-http.client :as http]
            [mount.core :refer [defstate]]))

(defstate env
  :start (go
           (let [response (<! (http/get "/api/config"))]
             (merge response (args)))))
