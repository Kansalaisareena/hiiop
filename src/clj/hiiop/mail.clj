(ns hiiop.mail
  (:require [postal.core :refer [send-message]]
            [clojure.core.async :refer [go]]
            [hiiop.config :refer [env]]))

(defn send-mail [settings mail]
  (go
    (send-message settings mail)))

(defn send-token [email token]
  (let [mailopts (get-in env [:aws :mail-server-opts])]
    (send-mail mailopts
               {:from (get-in env [:aws :sender-address])
                :to email
                :body token
                :subject "Hiiop password token"})))
