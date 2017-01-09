(ns hiiop.api-handlers
  (:require [clojure.pprint :as pp]
            [clojure.math.numeric-tower :as math]
            [taoensso.timbre :as log]
            [ring.util.http-response :refer :all]
            [mount.core :as mount]
            [buddy.auth :refer [authenticated?]]
            [buddy.hashers :as hashers]
            [buddy.core.codecs :as codecs]
            [buddy.core.codecs.base64 :as b64]
            [buddy.auth.http :as http]
            [cuerdas.core :as str]
            [compojure.api.sweet :as sweet]
            [conman.core :refer [with-transaction]]
            [schema.core :as s]
            [schema.coerce :as sc]
            [schema-tools.core :as st]
            [schema-tools.coerce :as stc]
            [hiiop.config :refer [env]]
            [hiiop.db.core :as db]
            [hiiop.coerce :as hc]
            [hiiop.time :as time]
            [hiiop.mail :as mail]
            [hiiop.file-upload :refer [upload-picture]]
            [hiiop.contentful :as cf]
            [hiiop.middleware :refer [wrap-simple-auth]]))

(defn login-status
  [request]
  (ok (authenticated? request)))

(defn show-session
  [request]
  (ok (str request)))

(defn logout
  [{session :session}]
  (assoc (ok) :session (dissoc session :identity)))

(defn login
  [{{:keys [email password]} :body-params session :session}]
  (let [password-ok (db/check-password email password)
        user-info (db/get-user-name-and-id {:email email})]
    (if password-ok
      (assoc (ok)
             :session (assoc session :identity user-info))
      (unauthorized))))

(defn register [{:keys [email name phone locale]}]
  (try
    (let [id (:id (db/create-virtual-user! {:email email}))]
      (if (nil? id)
        {:errors {:email :errors.email.in-use}}
        (let [token (db/create-password-token!
                     {:email email
                      :expires (time/add (time/now) time/an-hour)})]
          (db/update-user! {:id id
                            :name name
                            :email email
                            :phone phone
                            :locale (clojure.core/name locale)})
          (mail/send-activation-token-email email (str (:token token)) locale)
          id)))
    (catch Exception e
      (log/error e)
      {:errors {:email :errors.email.in-use}})))

(defn validate-token [token-uuid]
  (try
    (let [token-info (db/get-token-info {:token token-uuid})]
      (if (nil? token-info)
        {:errors {:token :errors.user.token.invalid}}
        token-info))
    (catch Exception e
      (log/error e)
      {:errors {:token :errors.user.token.invalid}})))


(defn activate [{:keys [password token]}]
  "Attempt to activate user with token. If successful, delete token,
  set password and set user active."
  (let [pwhash (hashers/derive password {:alg :bcrypt+blake2b-512})
        token-info (db/get-token-info {:token token})]
    (< 0
       (with-transaction [db/*db*]
         (db/activate-user!
          {:pass pwhash
           :email (:email token-info)
           :token token})
         (db/delete-password-token! {:token token})))))

(defn- send-password-reset-email [{:keys [user token] :as args}]
  (try
    (assoc args
           :email-sent
           (mail/send-password-reset-token-email
            (:email user)
            token
            (keyword (:locale user))))
    (catch Exception e
      (log/error e)
      (assoc args :email-sent false))))

(defn reset-password [email]
  (try
    (-> (assoc {} :user (db/get-user-by-email {:email email}))
        (assoc :token
               (:token (db/create-password-token!
                        {:email email
                         :expires (time/add (time/now) time/an-hour)})))
        (send-password-reset-email)
        (#(if (:email-sent %1)
            true
            {:errors {:password-reset :errors.password-reset.email-sending-failed}})))
    (catch Exception e
      (log/error e)
      {:errors {:password-reset :errors.password-reset.failed}})))

(defn change-password [{:keys [password token]}]
  (log/info "change-password" password token)
  (let [pwhash (hashers/derive password {:alg :bcrypt+blake2b-512})
        token-info (db/get-token-info {:token token})]
    (if token-info
      (= 1
         (with-transaction [db/*db*]
           (db/change-password! {:email (:email token-info) :pass pwhash :token token})
           (db/delete-password-token! {:token token})))
      {:errors {:token :errors.token.expired}})))

(defn get-user [id]
  (try
    (let [user-info (db/get-user-by-id {:id (sc/string->uuid id)})]
      (if (nil? user-info)
        {:errors {:user :errors.user.not-found}}
        user-info))
    (catch Exception e
      (log/error e)
      {:errors {:users :errors.user.not-found}})))

(defn add-quest [{:keys [quest user]}]
  (try
    (let [organiser-participates (:organiser-participates quest)
          max-days (+ (time/days-between
                       (time/from-string (:start-time quest))
                       (time/from-string (:end-time quest))) 1)]
      (-> quest
          (assoc :owner (:id user))
          (dissoc :organiser-participates)
          (hc/new-api-quest->new-db-quest-coercer)
          ((fn [quest]
             (with-transaction [db/*db*]
               (-> quest
                   (db/add-unmoderated-quest!)
                   ((fn [q]
                      (when organiser-participates
                        (db/join-quest!
                         (hc/api-party-member->db-party-member
                          {:quest-id (:id q)
                           :user-id (:id user)
                           :days max-days})))
                      q))))))
          (#(db/get-unmoderated-quest-by-id {:id (:id %1)
                                             :owner (:id user)}))
          (hc/db-quest->api-quest-coercer)))
    (catch Exception e
      (log/error e))))

(defn delete-quest [{:keys [id user]}]
  ;; TODO: currently only supports deleting moderated quest
  (try
    (let [quest (db/get-moderated-quest-by-id {:id id})
          owner (:owner quest)]
      (if (= (str owner) (str (:id user)))
        (db/delete-quest-by-id! {:id id})
        {:errors {:quest :errors.quest.not-authorised-to-delete-quest}}))
    (catch Exception e
      (log/error e)
      {:errors {:quest :errors.quest.failed-to-delete-quest}})))

(defn get-quest [id]
  (log/info "quest" id)
  (try
    (-> (db/get-moderated-quest-by-id {:id id})
        (hc/db-quest->api-quest-coercer))
    (catch Exception e
      (log/error e))))

(defn get-unmoderated-quest [id user-id]
  (try
    (-> (db/get-unmoderated-quest-by-id {:id id})
        (hc/db-quest->api-quest-coercer))
    (catch Exception e
      (log/error e))))

(defn get-secret-quest [{:keys [id secret-party]}]
  (try
    (-> (db/get-moderated-secret-quest
         (db/->snake_case_keywords
          {:id id
           :secret-party (sc/string->uuid secret-party)}))
        (hc/db-quest->api-quest-coercer))))

(defn edit-quest [{:keys [quest user]}]
  (log/info user)
  (try
    (-> quest
        (assoc :owner (:id user))
        (dissoc :organiser-participates)
        (hc/api-quest->db-quest-coercer)
        (db/update-quest!)
        (#(db/get-unmoderated-quest-by-id {:id (:id %) :owner (:id user)}))
        (#(if %1
            (hc/db-quest->api-quest-coercer %1)
            {:errors {:unauthorized :errors.unauthorized.title}})))
    (catch Exception e
      (log/error e)
      {:errors {:quest "Failed to update"}})))

(defn get-quests-for-owner [{:keys [owner]}]
  (try
    (-> (db/get-all-quests-by-owner {:owner owner})
        ((partial map hc/db-quest->api-quest-coercer)))
    (catch Exception e
      (log/error e)
      {:errors {:quests :errors.quest.unexpected-error}})))

(defn get-moderated-quests []
  (try
    (-> (db/get-all-moderated-quests)
        ((partial map hc/db-quest->api-quest-coercer)))
    (catch Exception e
      (log/error e)
      {:errors {:quests :error.quest.unexpected-error}})))

(defn get-participating-quests [{:keys [user-id]}]
  (try
    (-> (db/get-all-participating-quests {:user_id user-id})
        ((partial map hc/db-quest->api-quest-coercer)))
    (catch Exception e
      (log/error e)
      {:errors {:quests :error.quest.unexpected-error}})))

(defn get-user-quests [{:keys [user-id]}]
  (try
    (let [own-quests (get-quests-for-owner {:owner user-id})
          participating-quests (get-participating-quests {:user-id user-id})]
      (if (and (nil? (:errors own-quests))
               (nil? (:errors participating-quests)))
        {:organizing own-quests :attending participating-quests}
        {:errors {:quests :error.quest.unexpected-error}}))
    (catch Exception e
      (log/error e)
      {:errors {:quests :error.quest.unexpected-error}})))

(defn get-unmoderated-quests [{:keys [user-id]}]
  (try
    (-> (db/get-all-unmoderated-quests
         (db/->snake_case_keywords
          {:user-id user-id}))
        ((partial map hc/db-quest->api-quest-coercer)))
    (catch Exception e
      (log/error e)
      {:errors {:quests :error.quest.unexpected-error}})))

(defn- send-quest-accepted-email [{:keys [quest user] :as args}]
  (try
    (let [quest-accepted-email
          (if (:is-open quest)
            mail/send-quest-accepted-email
            mail/send-private-quest-accepted-email)]
      (assoc args
             :email
             (quest-accepted-email
              {:quest        quest
               :email        (:email user)
               :user         user
               :locale       (:locale user)
               :secret-party (:secret-party quest)})))
    (catch Exception e
      (log/error e)
      (assoc args :email false))))

(defn moderate-accept-quest [{:keys [quest-id user-id]}]
  (try
    (-> (db/moderate-accept-quest!
         (db/->snake_case_keywords {:id quest-id
                                    :user-id user-id}))
        (#(assoc {} :accepted-quest %1))
        (assoc :user (db/get-quest-owner {:id quest-id}))
        (assoc :quest
               (hc/db-quest->api-quest-coercer
                (db/get-moderated-quest-by-id {:id quest-id})))
        (assoc-in [:quest :secret-party]
                  (:secret-party (db/get-quest-secret-party
                                  {:id quest-id})))
        (send-quest-accepted-email)
        (:quest)
        (dissoc :secret-party))
    (catch Exception e
      (log/error e)
      {:errors {:quests :errors.unauthorized}})))

(defn- send-quest-rejected-email [{:keys [quest user message] :as args}]
  (try
    (assoc args
           :email
           (mail/send-quest-declined-email
            {:quest quest
             :user user
             :email (:email user)
             :message message
             :locale (:locale user)}))
    (catch Exception e
      (log/error e)
      (assoc args :email false))))

(defn moderate-reject-quest [{:keys [quest-id message user-id]}]
  (try
    (-> (db/moderate-reject-quest!
         (db/->snake_case_keywords {:id quest-id
                                    :user-id user-id}))
        (#(assoc {} :reject-quest %1))
        (assoc :user (db/get-quest-owner {:id quest-id}))
        (#(assoc %1 :quest
                 (hc/db-quest->api-quest-coercer
                  (db/get-unmoderated-quest-by-id
                   {:id quest-id
                    :owner (get-in %1 [:user :id])}))))
        (assoc :message message)
        (send-quest-rejected-email)
        (:quest))
    (catch Exception e
      (log/error e)
      {:errors {:moderation :errors.moderation.failed}}
      )))

(defn check-and-update-user-info [user-info]
  user-info)

(defn get-party-member [{:keys [member-id]}]
  (try
    (-> (db/get-party-member {:id member-id})
        (hc/db-party-member->api-party-member-coercer))
    (catch Exception e
      (log/error e))))

(defn get-quest-party [{:keys [quest-id user]}]
  (let [party-members (db/get-quest-party-members
                       (db/->snake_case_keywords
                        {:quest-id quest-id
                         :user-id (:id user)}))]
    (if party-members
      party-members
      {:errors {:party [:errrors.quest.not-found :or :errors.unauthorized]}})))

(defn use-existing-or-create-new-user! [{:keys [email name phone locale agreement]}]
  (with-transaction [db/*db*]
    (-> (db/get-user-by-email {:email email})
        ((fn [existing-user]
           (cond
             (not existing-user)
             (let [id (:id (db/create-virtual-user! {:email email}))
                   user  {:id id
                          :name name
                          :email email
                          :phone phone
                          :locale (clojure.core/name locale)}]
               (db/update-user! user)
               user)

             :else
             (check-and-update-user-info existing-user)))))))

(defn join-open-quest! [{:keys [quest_id user_id days] :as args}]
  (with-transaction [db/*db*]
    (if (:exists (db/can-join-open-quest? args))
      (db/join-quest! args)
      {:errors {:party :errors.quest.full}})))

(defn join-secret-quest! [{:keys [quest_id user_id days secret_party] :as args}]
  (log/info "join secret quest!" quest_id user_id days secret_party)
  (with-transaction [db/*db*]
    (if (:exists (db/can-join-secret-quest? args))
      (db/join-quest! args)
      {:errors {:party [:errors.quest.full :or :errors.quest.join.secret-key.incorrect]}})))

(defn party-member-or-errors [{:keys [quest-id new-member days session-user locale]}]
  (cond
    (nil? quest-id)
    {:errors {:quest :errors.not-found}}

    (and quest-id
         (:user-id new-member)
         (= (:user-id new-member) (:id session-user)))
    (do
      (hc/api-new-member->db-new-member-coercer
       (-> (conj new-member
                 {:quest-id quest-id})
           (assoc :days days))))

    (and (:user-id new-member)
         (not (= (:user-id new-member) (:id session-user))))
    {:errors {:user-id :user.not-logged-in}}

    (and quest-id (:signup new-member))
    (do
      (-> (use-existing-or-create-new-user!
           (assoc (:signup new-member) :locale locale))
          ((fn [user]
             (hc/api-new-member->db-new-member-coercer
              (-> (dissoc new-member :signup)
                  (assoc :user-id (:id user)
                         :quest-id quest-id
                         :days days)))))))

    :else
    {:errors {:signup :errors.not-found
              :user-id :errors-not-found}}))

(defn- send-join-email [{:keys [user quest member] :as args}]
  (try
    (assoc args :email
           (mail/send-join-quest-email
            {:email (:email user)
             :quest (hc/db-quest->api-quest-coercer quest)
             :locale (keyword (:locale user))
             :member-id (:member-id member)}))
    (catch Exception e
      (log/error e)
      (assoc args :email false))))

(defn join-quest [{:keys [id new-member user locale]}]
  (try
    (log/info "join-quest" id new-member user)
    (let [quest        (db/get-moderated-quest-by-id {:id id})
          quest-id     (:id quest)
          is-open      (:is-open quest)
          has-space    (> (:max-participants quest) (:participant-count quest))
          start-time   (:start-time quest)
          end-time     (:end-time quest)
          max-days     (+ (time/days-between start-time end-time) 1)
          usable-days  (or (when (>= max-days (:days new-member))
                             (:days new-member))
                           max-days)
          party-member (if has-space
                         (party-member-or-errors
                          {:quest-id quest-id
                           :days usable-days
                           :new-member new-member
                           :session-user user
                           :locale locale})
                         {:errors {:quest :errors.quest.full}})]

      (if (nil? (:errors party-member))
        (let [join-with (if is-open
                          join-open-quest!
                          join-secret-quest!)
              joined (join-with party-member)]
          (if (nil? (:errors joined))
            (do
              (log/info joined)
              (-> (db/get-user-by-id {:id (:user_id party-member)})
                  (#(assoc {} :user %1))
                  (assoc :member (db/get-party-member joined))
                  (#(send-join-email
                     {:user (:user %1)
                      :quest quest
                      :member (:member %1)}))
                  (:member)
                  (hc/db-party-member->api-party-member-coercer)))
            joined))
        party-member))
    (catch Exception e
      (log/error e)
      {:errors {:party :errors.join-failed}})))

(defn remove-party-member [{:keys [member-id]}]
  (try
    (db/remove-member-from-party!
     (db/->snake_case_keywords
      {:member-id member-id}))
  (catch Exception e
    (log/error e))))

(defn get-picture [id])

(defn picture-supported? [file]
  (-> (:content-type file)
      (#(assoc {}
               :type
               (not (re-find #"^image/(jpg|jpeg|png|gif)$" %1))))
      (assoc :size
             (> (:size file) (* 3 (math/expt 10 6))))
      (#(cond
          (:size %1)
          {:errors {:picture :errors.picture.too-big}}

          (:type %1)
          {:errors {:picture :errors.picture.type-not-supported
                    :type (:content-type file)}}

          :else
          true
          ))))

(defn add-picture [{:keys [file user]}]
  (let [picture-supported (picture-supported? file)]
    (if (not (:errors picture-supported))
      (try
        (let [picture-id (:id
                          (db/add-picture! {:url ""
                                            :owner (:id user)}))]
          (-> picture-id
              (upload-picture file)
              (#(db/update-picture-url! {:id picture-id
                                         :url %1}))
              ((fn [db-reply]
                 (log/info db-reply)
                 {:id picture-id
                  :url (:url db-reply)}))
              ))
        (catch Exception e
          (log/error e)
          {:errors {:picture :errors.picture.add-failed}}))
      picture-supported)))
