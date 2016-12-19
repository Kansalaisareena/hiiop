(ns hiiop.api-handlers
  (:require [clojure.pprint :as pp]
            [hiiop.config :refer [env]]
            [ring.util.http-response :refer :all]
            [mount.core :as mount]
            [buddy.auth :refer [authenticated?]]
            [buddy.hashers :as hashers]
            [compojure.api.sweet :as sweet]
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
        {:errors {:user :errors.user.register.failed}}
        (let [token (db/create-password-token!
                     {:email email
                      :expires (time/add (time/now) time/an-hour)})]
          (db/update-user! {:id id :name name :email email})
          (mail/send-token-email email (str (:token token)) locale)
          id)))
    (catch Exception e
      (log/error e)
      {:errors {:user :errors.user.register.failed}})))

(defn activate [{:keys [email password token]}]
  (let [pwhash (hashers/derive password {:alg :bcrypt+blake2b-512})
        token-uuid (sc/string->uuid token)]
    (db/activate-user! {:pass pwhash :email email :token token-uuid})))

(defn validate-token [request]
  (let [token (:token (:body-params request))]
    (try
      (let [token-uuid (sc/string->uuid token)
            token-info (db/get-token-info {:token token-uuid})]
        (if (nil? token-info)
          {:errors {:token :errors.user.token.invalid}}
          token-info))
      (catch Exception e
        (log/error e)
        {:errors {:token :errors.user.token.invalid}}))))

(defn get-user [{{id :id} :params}]
  (ok (str (db/get-user-by-id {:id (sc/string->uuid id)}))))

(defn api-quest->db-quest
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
      (assoc :picture picture-id)
      (assoc :organisation (:name organisation))
      (assoc :organisation-description (:description organisation))
      (conj (:coordinates location))
      (conj location)
      (assoc :street-number (:street-number location))
      (dissoc :location :coordinates :picture-id)
      (db/->snake_case_keywords))
  )

(def DBQuest
  {:id hs/NaturalNumber
   :name hs/NonEmptyString
   :description (s/maybe hs/NonEmptyString)
   :organisation (s/maybe hs/NonEmptyString)
   :organisation_description (s/maybe hs/NonEmptyString)
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
   :unmoderated_description hs/NonEmptyString
   :max_participants s/Num
   :hashtags [s/Str]
   :picture (s/maybe s/Uuid)
   :owner s/Uuid
   :is_open s/Bool
   :secret_party s/Uuid})

(def NewDBQuest
  (st/dissoc DBQuest
             :id
             :description
             :secret_party))

(def api-quest->db-quest-coercer
  (stc/coercer NewDBQuest
               {NewDBQuest api-quest->db-quest}))

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
                                   organisation-description] :as db-quest}]
  (-> db-quest
      (location-flat->location-structure)
      (times->strings)
      (string-categories->keyword-categories)
      (assoc :organisation
             {:name organisation :description organisation-description})
      (dissoc :organisation :organisation-description)))

(def db-quest->api-quest-coercer
  (stc/coercer hs/Quest
               {hs/Quest db-quest->api-quest}))

(defn add-quest [{:keys [quest user]}]
  (try
    (-> quest
        (assoc :owner (:id user))
        (dissoc :organiser-participates)
        (api-quest->db-quest-coercer)
        (db/add-unmoderated-quest!)
        (#(db/get-quest-by-id {:id (:id %)}))
        (db-quest->api-quest-coercer))
    (catch Exception e
      (log/error e)
      )))

(defn get-quest [{{id :id} :params}]
  (ok (db/get-quest-by-id {:id id})))

(defn join-quest [params]
  (created ""))

(defn get-picture [id])

(defn picture-supported? [file]
  (-> (:content-type file)
      (#(re-find #"^image/(jpg|jpeg|png|gif)$" %1))))

(defn add-picture [file]
  (if (picture-supported? file)
    (try
      (let [picture-id (:id (db/add-picture! {:url ""}))]
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
