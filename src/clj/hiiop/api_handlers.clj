(ns hiiop.api-handlers
  (:require [clojure.pprint :as pp]
            [hiiop.config :refer [env]]
            [ring.util.http-response :refer :all]
            [mount.core :as mount]
            [buddy.auth :refer [authenticated?]]
            [buddy.hashers :as hashers]
            [compojure.api.sweet :as sweet]
            [conman.core :refer [with-transaction]]
            [taoensso.timbre :as log]
            [schema.core :as s]
            [schema-tools.core :as st]
            [schema-tools.coerce :as stc]
            [schema.coerce :as sc]
            [hiiop.db.core :as db]
            [hiiop.time :as time]
            [hiiop.mail :as mail]
            [hiiop.schema :as hs]
            [hiiop.file-upload :refer [upload-picture]]
            [hiiop.contentful :as cf]
            [buddy.core.codecs :as codecs]
            [buddy.core.codecs.base64 :as b64]
            [buddy.auth.http :as http]
            [cuerdas.core :as str]))

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
        user-id (db/get-user-id {:email email})]
    (if password-ok
      (assoc (ok)
             :session (assoc session :identity user-id))
      (unauthorized))))

(defn register [{:keys [email name locale]}]
  (try
    (let [id (:id (db/create-virtual-user! {:email email}))]
      (if (nil? id)
        {:errors {:email :errors.email.in-use}}
        (let [token (db/create-password-token!
                     {:email email
                      :expires (time/add (time/now) time/an-hour)})]
          (db/update-user! {:id id :name name :email email})
          (mail/send-token-email email (str (:token token)) locale)
          id)))
    (catch Exception e
      (log/error e)
      {:errors {:email :errors.email.in-use}})))

(defn validate-token [token-uuid]
  (log/info "validate-token" token-uuid)
  (try
    (let [token-info (db/get-token-info {:token token-uuid})]
      (if (nil? token-info)
        {:errors {:token :errors.user.token.invalid}}
        token-info))
    (catch Exception e
      (log/error e)
      {:errors {:token :errors.user.token.invalid}})))


(defn activate [{:keys [email password token]}]
  "Attempt to activate user with token. If successful, delete token,
  set password and set user active."
  (let [pwhash (hashers/derive password {:alg :bcrypt+blake2b-512})
        token-uuid (sc/string->uuid token)]
    (< 0 (with-transaction [db/*db*]
           (db/activate-user! {:pass pwhash :email email :token token-uuid})
           (db/delete-password-token! {:token token-uuid})))))


(defn reset-password [email]
  (db/create-password-token!
   {:email email :expires (time/add (time/now) time/an-hour)}))

(defn change-password [{:keys [email password token]}]
  (let [pwhash (hashers/derive password {:alg :bcrypt+blake2b-512})]
    (= 1 (with-transaction [db/*db*]
           (db/change-password! {:email email :pass pwhash :token token})
           (db/delete-password-token! {:token token})))))

(defn get-user [id]
  (try
    (let [user-info (db/get-user-by-id {:id (sc/string->uuid id)})]
      (if (nil? user-info)
        {:errors {:user :errors.user.not-found}}
        user-info))
    (catch Exception e
      (log/error e)
      {:errors {:users :errors.user.not-found}})))

(def moderated->unmoderated-db-quest-keys
  {:name                     :unmoderated_name
   :description              :unmoderated_description
   :organisation             :unmoderated_organisation
   :organisation_description :unmoderated_organisation_description
   :hashtags                 :unmoderated_hashtags
   :picture                  :unmoderated_picture})

(defn api-quest->moderated-db-quest
  [{:keys [hashtags
           start-time
           end-time
           location
           categories
           picture-id
           organisation
           organiser-participates] :as quest-from-api}]
  (-> quest-from-api
      (assoc :hashtags (when hashtags (vec (distinct hashtags))))
      (assoc :categories (vec (distinct categories)))
      (assoc :start-time  (time/from-string start-time))
      (assoc :end-time (time/from-string end-time))
      (assoc :picture (when (not (= picture-id ""))
                        (sc/string->uuid picture-id)))
      (assoc :organisation (:name organisation))
      (assoc :organisation-description (:description organisation))
      (conj (:coordinates location))
      (conj location)
      (assoc :street-number (:street-number location))
      (dissoc :location :coordinates :picture-id :picture-url)
      (db/->snake_case_keywords))
  )

(defn api-quest->new-unmoderated-db-quest [])

(def DBQuest
  {:id hs/NaturalNumber
   :name hs/NonEmptyString
   :unmoderated_name hs/NonEmptyString
   :description hs/NonEmptyString
   :unmoderated_description hs/NonEmptyString
   :organisation (s/maybe hs/NonEmptyString)
   :unmoderated_organisation (s/maybe hs/NonEmptyString)
   :organisation_description (s/maybe hs/NonEmptyString)
   :unmoderated_organisation_description (s/maybe hs/NonEmptyString)
   :start_time (s/constrained s/Any time/time? :error.not-valid-date)
   :end_time (s/constrained s/Any time/time? :error.not-valid-date)
   :street_number (s/maybe s/Int)
   :street (s/maybe hs/NonEmptyString)
   :town hs/NonEmptyString
   :postal_code hs/NonEmptyString
   :country hs/NonEmptyString
   :latitude (s/maybe hs/NonEmptyString)
   :longitude (s/maybe hs/NonEmptyString)
   :google_maps_url (s/maybe hs/NonEmptyString)
   :google_place_id (s/maybe hs/NonEmptyString)
   :categories [s/Keyword]
   :max_participants s/Num
   :hashtags [s/Str]
   :unmoderated_hashtags [s/Str]
   :picture (s/maybe s/Uuid)
   :unmoderated_picture (s/maybe s/Uuid)
   :owner s/Uuid
   :is_open s/Bool})

(def UnmoderatedDBQuest
  (apply
   st/dissoc
   (concat
    [DBQuest]
    (keys moderated->unmoderated-db-quest-keys))))

(def ModeratedDBQuest
  (apply
   st/dissoc
   (concat
    [DBQuest]
    (vals moderated->unmoderated-db-quest-keys))))

(def NewUnmoderatedDBQuest
  (st/dissoc UnmoderatedDBQuest
             :id
             :secret_party))

(def NewModeratedDBQuest
  (st/dissoc ModeratedDBQuest
             :id
             :secret_party))

(def api-quest->moderated-db-quest-coercer
  (stc/coercer ModeratedDBQuest
               {ModeratedDBQuest api-quest->moderated-db-quest}))

(def new-api-quest->new-moderated-db-quest-coercer
  (stc/coercer NewModeratedDBQuest
               {NewModeratedDBQuest api-quest->moderated-db-quest}))

(def new-api-quest->new-unmoderated-db-quest-coercer
  (stc/coercer NewUnmoderatedDBQuest
               {NewUnmoderatedDBQuest api-quest->new-unmoderated-db-quest}))


(def location-keys
  [:street-number
   :street
   :town
   :postal-code
   :country
   :longitude
   :latitude
   :google-place-id
   :google-maps-url])

(defn location-flat->location-structure [db-quest]
  (let [location-data (select-keys db-quest location-keys)
        coordinates (when
                        (and
                         (:latitude location-data)
                         (:longitude location-data))
                      {:coordinates
                       {:latitude (:latitude location-data)
                        :longitude (:longitude location-data)}})
        location-data-no-lat-lon (dissoc location-data :latitude :longitude)
        location-structure (conj location-data-no-lat-lon coordinates)
        quest-no-location-data (apply dissoc db-quest location-keys)]
    (conj quest-no-location-data {:location location-structure})))

(defn time->string [keyword map]
  (time/to-string (keyword map)))

(defn times->strings [map]
  (assoc map
         :start-time (time->string :start-time map)
         :end-time   (time->string :end-time map)))

(defn string-categories->keyword-categories [db-quest]
  (assoc db-quest :categories
         (into [] (map keyword (:categories db-quest)))))

(defn db-quest->api-quest [{:keys [organisation
                                   organisation-description
                                   picture] :as db-quest}]
  (-> db-quest
      (location-flat->location-structure)
      (times->strings)
      (string-categories->keyword-categories)
      (dissoc :picture)
      (assoc :picture-id (str picture))
      (dissoc :organisation :organisation-description)
      (#(if organisation
          (assoc %1 :organisation
                 {:name organisation :description organisation-description})
          %1))))

(def db-quest->api-quest-coercer
  (stc/coercer hs/Quest
               {hs/Quest db-quest->api-quest}))

(defn add-quest [{:keys [quest user]}]
  (try
    (-> quest
        (assoc :owner (:id user))
        (dissoc :organiser-participates)
        (new-api-quest->new-moderated-db-quest-coercer)
        (db/add-moderated-quest!)
        (#(db/get-moderated-quest-by-id {:id (:id %)}))
        (db-quest->api-quest-coercer))
    (catch Exception e
      (log/error e)
      )))

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
  (try
    (-> (db/get-moderated-quest-by-id {:id id})
        (db-quest->api-quest-coercer))
    (catch Exception e
      (log/error e))))

(defn edit-quest [{:keys [quest user]}]
  (try
    (-> quest
        (assoc :owner (:id user)) ;; TODO When moderating, this is different!
        (dissoc :organiser-participates)
        (api-quest->moderated-db-quest-coercer)
        (db/update-moderated-quest!)
        (#(db/get-moderated-quest-by-id {:id (:id %)}))
        (db-quest->api-quest-coercer))
    (catch Exception e
      (log/error e)
      {:errors {:quest "Failed to update"}})))

(defn get-quests-for-owner [owner]
  (try
    ;; TODO: the profile page using this handler to get all moderated
    ;; and unmoderated quests for the user. But we only have moderated quests for now
    (-> (db/get-moderated-quests-by-owner {:owner owner})
        ((partial map db-quest->api-quest-coercer)))
    (catch Exception e
      (log/error e)
      {:errors {:quests :errors.quest.unexpected-error}})))

(defn join-quest [params]
  (created ""))

(defn get-picture [id])

(defn picture-supported? [file]
  (-> (:content-type file)
      (#(re-find #"^image/(jpg|jpeg|png|gif)$" %1))))

(defn add-picture [{:keys [file user]}]
  (if (picture-supported? file)
    (try
      (let [picture-id (:id
                        (db/add-picture! {:url ""
                                          :owner (:id user)}))]
        (log/info picture-id)
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
    {:errors {:picture :errors.picture.type-not-supported
              :type (:content-type file)}}))

(defn hook-auth
  "Check the response for correct credentials for webhook"
  [request]
  (let [pattern (re-pattern "^Basic (.+)$")
        decoded (some->> (http/-get-header request "authorization")
                         (re-find pattern)
                         (second)
                         (b64/decode)
                         (codecs/bytes->str))]
    (let [[username password] (str/split decoded #":" 2)]
      (and (= username (get-in env [:contentful :webhook-user]))
           (= password (get-in env [:contentful :webhook-password]))))))

(defn contentful-hook [{cfobject :body-params :as request}]
  (if (hook-auth request)
    (cf/process-item cfobject)
    (unauthorized)))
