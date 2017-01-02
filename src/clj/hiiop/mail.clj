(ns hiiop.mail
  (:require [clojure.core.async :refer [go]]
            [postal.core :refer [send-message]]
            [rum.core :refer [render-static-markup]]
            [taoensso.carmine :as car]
            [taoensso.timbre :as log]
            [mount.core :refer [defstate]]
            [bidi.bidi :refer [path-for]]
            [hiiop.translate :as ht]
            [hiiop.contentful :as cf]
            [hiiop.emails :as emails]
            [hiiop.url :refer [url-to]]
            [hiiop.redis :refer [wcar*]]
            [hiiop.config :refer [env]]))

(defstate url-to' :start (partial url-to (:site-base-url env)))

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
                 {:button-url (url-to' :activate :token (str token))
                  :title (content :otsikko)
                  :body-text (content :leipateksti)
                  :button-text (content :ekanappiteksti)}}))))

(defn send-password-reset-token [email token locale]
  (let [content (mail-content "password-reset" locale)]
    (send-mail
     (make-mail {:to email
                 :subject (content :otsikko)
                 :template emails/simple-mail
                 :template-params
                 {:button-url (url-to' :index)
                  :title (content :otsikko)
                  :body-text (content :leipateksti)
                  :button-text (content :ekanappiteksti)}}))))

(defn send-new-quest [{:keys [email quest locale]}]
  (let [content (mail-content "new-quest" locale)]
    (send-mail
     (make-mail {:to email
                 :subject (content :otsikko)
                 :template emails/simple-mail
                 :template-params
                 {:button-url (url-to' :quest :quest-id (:id quest))
                  :title (content :otsikko)
                  :body-text (content :leipateksti)
                  :button-text (content :ekanappiteksti)}}))))

(defn send-edit-quest [email quest-id locale]
  (let [content (mail-content "edit-quest" locale)]
    (send-mail
     (make-mail {:to email
                 :subject (content :otsikko)
                 :template emails/simple-mail
                 :template-params
                 {:button-url (url-to' :quest :quest-id quest-id)
                  :title (content :otsikko)
                  :body-text (content :leipateksti)
                  :button-text (content :ekanappiteksti)}}))))

(defn send-join-quest [{:keys [email quest locale]}]
  (let [tr (ht/tr-with [locale])
        quest-id (:id quest)
        content (mail-content "join-quest" locale)
        title (str (content :otsikko) " " (quest :name))]
    (send-mail
     (make-mail {:to email
                 :subject title
                 :template emails/quest-details-mail
                 :template-params
                 {:button-url (url-to' :quest :quest-id quest-id)
                  :title title
                  :tr tr
                  :quest quest
                  :body-text
                  (content :leipateksti)
                  :button-text (content :ekanappiteksti)}}))))

(defn send-quest-declined [email quest-id message locale]
  (let [content (mail-content "quest-declined" locale)]
    (send-mail
     (make-mail {:to email
                 :subject (content :otsikko)
                 :template emails/simple-mail
                 :template-params
                 {:button-url (url-to' :quest :quest-id quest-id)
                  :title (content :otsikko)
                  :body-text (content :leipateksti)
                  :button-text (content :ekanappiteksti)
                  :message message}}))))

(defn send-quest-accepted [email quest locale]
  (let [content (mail-content "quest-accepted" locale)
        title (str (content :otsikko) (:name quest))]
    (send-mail
     (make-mail {:to email
                 :subject title
                 :template emails/quest-details-mail
                 :template-params
                 {:button-url (url-to' :quest :quest-id (:id quest))
                  :title title
                  :quest quest
                  :body-text (content :leipateksti)
                  :button-text (content :ekanappiteksti)}}))))

(defn send-private-quest-accepted [{:keys [email quest locale]}]
  (let [content (mail-content "private-quest-accepted" locale)
        title (str (content :otsikko) (:name quest))]
    (send-mail
     (make-mail {:to email
                 :subject title
                 :template emails/quest-details-mail
                 :template-params
                 {:title (content :otsikko)
                  :body-text (content :leipateksti)
                  :button-text (content :ekanappiteksti)
                  :button-url (url-to' :quest :quest-id (:id quest))
                  :button2-text (content :tokanappiteksti)
                  :button2-url (url-to' :secret-quest
                                        :quest-id (:id quest)
                                        :secret-party (:secret-party quest))}}))))

(defn send-quest-deleted [email quest-id locale]
  (let [content (mail-content "quest-deleted" locale)]
    (send-mail
     (make-mail {:to email
                 :subject (content :otsikko)
                 :template emails/simple-mail
                 :template-params
                 {:button-url (url-to' :quest :quest-id quest-id)
                  :title (content :otsikko)
                  :body-text (content :leipateksti)
                  :button-text (content :ekanappiteksti)}}))))

(defstate send-activation-token-email       :start send-activation-token)
(defstate send-password-reset-token-email   :start send-password-reset-token)
(defstate send-new-quest-email              :start send-new-quest)
(defstate send-edit-quest-email             :start send-edit-quest)
(defstate send-join-quest-email             :start send-join-quest)
(defstate send-quest-declined-email         :start send-quest-declined)
(defstate send-quest-accepted-email         :start send-quest-accepted)
(defstate send-private-quest-accepted-email :start send-private-quest-accepted)
(defstate send-quest-deleted-email          :start send-quest-deleted)

