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


(defstate url-to :start
  (fn [leaf-name & args]
    (str (:site-base-url env)
         (apply path-for
                hierarchy
                leaf-name
                args))))

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

(defn make-mail [{:keys [to subject template template-params locale]}]
  {:from (:sender email-sending-config)
   :to to
   :subject subject
   :body [{:type "text/html; charset=utf-8"
           :content (render-static-markup
                     (template template-params))}]})

(defn mail-content [emailkey locale]
  (-> (car/get (str "email:" emailkey))
      (wcar*)
      (:fields)
      (dissoc :emailkey)
      (cf/localize-fields locale)))

(defn send-activation-token [email token locale]
  (let [content (mail-content "activation" locale)]
    (send-mail
     (make-mail {:to email
                 :subject (content :otsikko)
                 :template emails/simple-mail
                 :template-params
                 {:button-url (url-to :activate :token (str token))
                  :title (content :otsikko)
                  :body-text (content :leipateksti)
                  :button-text (content :ekanappiteksti)}}))))

(defstate send-token-email :start send-token)
(defstate send-activation-token-email :start send-activation-token)
