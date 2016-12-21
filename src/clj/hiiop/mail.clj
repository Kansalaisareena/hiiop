(ns hiiop.mail
  (:require [clojure.core.async :refer [go]]
            [postal.core :refer [send-message]]
            [rum.core :refer [render-static-markup]]
            [taoensso.carmine :as car]
            [taoensso.timbre :as log]
            [mount.core :refer [defstate]]
            [bidi.bidi :refer [path-for]]
            [hiiop.contentful :as cf]
            [hiiop.emails :as emails]
            [hiiop.routes.page-hierarchy :refer [hierarchy]]
            [hiiop.redis :refer [wcar*]]
            [hiiop.config :refer [env]]))

(defstate email-sending-config :start
  (let [email-config {:host    (:smtp-server env)
                      :port    (:smtp-port env)
                      :user    (:smtp-user env)
                      :pass    (:smtp-password env)
                      :sender  (:sender-address env)}]
    (log/info "starting with email config" email-config)
    email-config))

(defn send-mail [mail]
  (log/info "sending mail" mail)
  (go
    (send-message email-sending-config mail)))

(defn mail-content [emailkey locale]
  (-> (car/get (str "email:" emailkey))
      (wcar*)
      (:fields)
      (dissoc :emailkey)
      (cf/localize-fields locale)))

(defn send-token [email token locale]
  (let [content (mail-content "activation" locale)]
    (send-mail {:from (:sender email-sending-config)
                :to email
                :body [{:type "text/html; charset=utf-8"
                        :content
                        (render-static-markup
                         (emails/activate-account
                          {:activation-url
                           (str (:site-base-url env)
                                (path-for hierarchy
                                          :activate
                                          :token (str token)))
                           :title (content :otsikko)
                           :body-text (content :leipateksti)
                           :button-text (content :ekanappiteksti)
                           }))}]
                :subject (content :otsikko)})))

(defstate send-token-email :start send-token)
