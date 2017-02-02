(ns hiiop.client-config
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<!]]
            [cljs-http.client :as http]
            [mount.core :refer [defstate args]]))

(defstate env
  :start
  (go
    (let [epoch (.getTime (new js/Date))
          response (<! (http/get
                        (str "/api/v1/config?t=" epoch)))]
      (merge (:body response) (args)))))
