(ns hiiop.mail
  (:require [clojure.core.async :refer [go]]
            [postal.core :refer [send-message]]
            [rum.core :refer [render-static-markup]]
            [taoensso.carmine :as car]
            [taoensso.timbre :as log]
            [mount.core :refer [defstate]]
            [hiiop.config :refer [env]]
            [hiiop.contentful :as cf]
            [hiiop.emails :as emails]
            [hiiop.redis :refer [wcar*]]))

(defn send-mail [mail]
  (let [mailopts (get-in env [:aws :mail-server-opts])]
    (go
      (send-message mailopts mail))))

(defn mail-content [emailkey locale]
  (-> (car/get (str "email:" emailkey))
      (wcar*)
      (:fields)
      (dissoc :emailkey)
      (cf/localize-fields locale)))

(defn send-token [email token locale]
  (let [content (mail-content "activation" locale)]
    (send-mail {:from (get-in env [:aws :sender-address])
                :to email
                :body [{:type "text/html"
                        :content (render-static-markup
                                  (emails/activate-account
                                   {:activation-url (str token)
                                    :title (content :otsikko)
                                    :body-text (content :leipateksti)
                                    :button-text (content :ekanappiteksti)
                                    }))}]
                :subject (content :otsikko)})))

(defstate send-token-email :start send-token)
