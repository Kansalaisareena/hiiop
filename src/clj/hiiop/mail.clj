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

(defn make-mail [{:keys [to subject template plaintext-template template-params locale]}]
  {:from (:sender email-sending-config)
   :to to
   :subject subject
   :body [:alternative
          {:type "text/html; charset=utf-8"
           :content (render-static-markup
                     (template template-params))}
          {:type "text/plain"
           :content (plaintext-template template-params)}]})

(defn mail-content [emailkey locale]
  (-> (car/get (str "email:" emailkey))
      (wcar*)
      (:fields)
      (dissoc :emailkey)
      (cf/localize-fields locale)))

(defn quest-email-title [title quest-name]
  (str title " " quest-name))

(defn- send-activation-token [email token locale]
  (let [content (mail-content "activation" locale)]
    (send-mail
     (make-mail {:to email
                 :subject (content :otsikko)
                 :template emails/simple-mail
                 :plaintext-template emails/plaintext-simple-mail
                 :template-params
                 {:button-url (url-to' :activate :token (str token))
                  :title (content :otsikko)
                  :body-text (content :leipateksti-rendered)
                  :body-text-plaintext (content :leipateksti)
                  :button-text (content :ekanappiteksti)}}))))

(defn- send-password-reset-token [email token locale]
  (let [content (mail-content "password-reset" locale)]
    (send-mail
     (make-mail {:to email
                 :subject (content :otsikko)
                 :template emails/simple-mail
                 :plaintext-template emails/plaintext-simple-mail
                 :template-params
                 {:title (content :otsikko)
                  :body-text (content :leipateksti-rendered)
                  :body-text-plaintext (content :leipateksti)
                  :button-text (content :ekanappiteksti)
                  :button-url (url-to' :password-reset :token token)}}))))

(defn- send-edit-quest [email quest-id locale]
  (let [content (mail-content "edit-quest" locale)]
    (send-mail
     (make-mail {:to email
                 :subject (content :otsikko)
                 :template emails/simple-mail
                 :plaintext-template emails/plaintext-simple-mail
                 :template-params
                 {:button-url (url-to' :quest :quest-id quest-id)
                  :title (content :otsikko)
                  :body-text (content :leipateksti-rendered)
                  :body-text-plaintext (content :leipateksti)
                  :button-text (content :ekanappiteksti)}}))))

(defn- send-join-quest [{:keys [email quest locale member-id]}]
  (log/info "send-join-quest" email quest locale member-id)
  (let [tr (ht/tr-with [locale])
        quest-id (:id quest)
        content (mail-content "join-quest" locale)
        title (quest-email-title (content :otsikko) (quest :name))]
    (send-mail
     (make-mail {:to email
                 :subject title
                 :template emails/quest-details-mail
                 :plaintext-template emails/plaintext-quest-details-mail
                 :template-params
                 {:tr tr
                  :title title
                  :quest quest
                  :body-text (content :leipateksti-rendered)
                  :body-text-plaintext (content :leipateksti)
                  :button-text (content :ekanappiteksti)
                  :button-url (url-to' :quest :quest-id quest-id)
                  :button2-text (content :tokanappiteksti)
                  :button2-url (url-to' :part-quest-party
                                        :quest-id quest-id
                                        :member-id member-id)}}))))

(defn- send-new-quest-participant [{:keys [email quest locale]}]
  (log/info "send-quest-new-participant" email quest)
  (let [quest-id (:id quest)
        content (mail-content "new-quest-participant" locale)
        title (quest-email-title (content :otsikko) (quest :name))]
    (send-mail
     (make-mail {:to email
                 :subject title
                 :template emails/simple-mail
                 :plaintext-template emails/plaintext-simple-mail
                 :template-params
                 {:title title
                  :body-text (content :leipateksti-rendered)
                  :body-text-plaintext (content :leipateksti)
                  :button-text (content :ekanappiteksti)
                  :button-url (url-to' :quest :quest-id quest-id)}}))))

(defn- send-quest-declined [{:keys [email quest message locale]}]
  (log/info email quest message locale)
  (let [tr (ht/tr-with [locale])
        quest-id (:id quest)
        content (mail-content "quest-declined" locale)
        title (quest-email-title (content :otsikko) (quest :name))]
    (send-mail
     (make-mail {:to email
                 :subject title
                 :template emails/quest-details-mail
                 :plaintext-template emails/plaintext-quest-details-mail
                 :template-params
                 {:tr tr
                  :title title
                  :quest quest
                  :body-text (content :leipateksti-rendered)
                  :body-text-plaintext (content :leipateksti)
                  :message message}}))))

(defn- send-quest-accepted [{:keys [email quest locale]}]
  (log/info "send-quest-accepted" email quest locale)
  (let [tr (ht/tr-with [locale])
        quest-id (:id quest)
        content (mail-content "quest-accepted" locale)
        title (quest-email-title (content :otsikko) (:name quest))]
    (send-mail
     (make-mail {:to email
                 :subject title
                 :template emails/quest-details-mail
                 :plaintext-template emails/plaintext-quest-details-mail
                 :template-params
                 {:tr tr
                  :title title
                  :quest quest
                  :body-text (content :leipateksti-rendered)
                  :body-text-plaintext (content :leipateksti)
                  :button-text (content :ekanappiteksti)
                  :button-url (url-to' :quest :quest-id (:id quest))}}))))

(defn- send-private-quest-accepted [{:keys [email quest locale]}]
  (log/info "send-private-quest-accepted" email quest locale)
  (let [tr (ht/tr-with [locale])
        quest-id (:id quest)
        secret-party (:secret-party quest)
        content (mail-content "private-quest-accepted" locale)
        title (str (content :otsikko) (:name quest))]
    (send-mail
     (make-mail {:to email
                 :subject title
                 :template emails/quest-details-mail
                 :plaintext-template emails/plaintext-quest-details-mail
                 :template-params
                 {:tr tr
                  :title (content :otsikko)
                  :quest quest
                  :body-text (content :leipateksti-rendered)
                  :body-text-plaintext (content :leipateksti)
                  :button-text (content :ekanappiteksti)
                  :button-url (url-to' :quest :quest-id quest-id)
                  :button2-text (content :tokanappiteksti)
                  :button2-url (url-to' :secret-quest
                                        :quest-id quest-id
                                        :secret-party secret-party)}}))))

(defn- send-quest-deleted [email quest-id locale]
  (let [content (mail-content "quest-deleted" locale)]
    (send-mail
     (make-mail {:to email
                 :subject (content :otsikko)
                 :template emails/simple-mail
                 :plaintext-template emails/plaintext-simple-mail
                 :template-params
                 {:button-url (url-to' :quest :quest-id quest-id)
                  :title (content :otsikko)
                  :body-text (content :leipateksti-rendered)
                  :body-text-plaintext (content :leipateksti)
                  :button-text (content :ekanappiteksti)}}))))

(defstate send-activation-token-email       :start send-activation-token)
(defstate send-password-reset-token-email   :start send-password-reset-token)
(defstate send-new-quest-participant-email  :start send-new-quest-participant)
(defstate send-edit-quest-email             :start send-edit-quest)
(defstate send-join-quest-email             :start send-join-quest)
(defstate send-quest-declined-email         :start send-quest-declined)
(defstate send-quest-accepted-email         :start send-quest-accepted)
(defstate send-private-quest-accepted-email :start send-private-quest-accepted)
(defstate send-quest-deleted-email          :start send-quest-deleted)

