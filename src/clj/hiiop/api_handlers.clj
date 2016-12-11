(ns hiiop.api-handlers
  (:require [clojure.pprint :as pp]
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
            [hiiop.schema :as hs]))

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

(defn register
  [{{email :email} :body-params}]
  (let [id (:id (db/create-virtual-user! {:email email}))]
    (if (nil? id)
      nil
      (let [token (:token (db/create-password-token!
                           {:email email :expires (time/add (time/now) time/an-hour)}))]
        (mail/send-token-email email (str token))
        (str id)))))

(defn activate
  [{{:keys [email password token]} :body-params}]
  (let [pwhash (hashers/derive password {:alg :bcrypt+blake2b-512})
        token-uuid (sc/string->uuid token)]
    (db/activate-user! {:pass pwhash :email email :token token-uuid})
    (ok)))

(defn get-user [{{id :id} :params}]
  (ok (str (db/get-user-by-id {:id (sc/string->uuid id)}))))

(defn api-quest->db-quest
  [{:keys [hashtags
           start-time
           end-time
           location
           categories
           picture-id
           organiser-participates] :as quest-from-api}]
  (let [distinct-hashtags (when hashtags (vec (distinct hashtags)))
        distinct-categories (vec (distinct categories))
        start-time (time/from-string start-time)
        end-time (time/from-string end-time)
        coordinates (:coordinates location)
        location-with-coordinates-flat (conj
                                        (dissoc location :coordinates)
                                        coordinates)
        with-location (conj
                       (dissoc quest-from-api :location)
                       location-with-coordinates-flat)
        with-modified-fields (assoc with-location
                                    :start-time start-time
                                    :end-time end-time
                                    :categories distinct-categories
                                    :hashtags distinct-hashtags
                                    :picture picture-id)
        also-without-picture-id (dissoc with-modified-fields
                                        :picture-id)
        also-without-picture-id (dissoc with-modified-fields
                                        :picture-id)
        quest-to-db (db/->snake_case_keywords also-without-picture-id)]
    quest-to-db
    ))

(def DBQuest
  {:id hs/NaturalNumber
   :name hs/NonEmptyString
   :description (s/maybe hs/NonEmptyString)
   :start_time (s/constrained s/Any time/time? :error.not-valid-date)
   :end_time (s/constrained s/Any time/time? :error.not-valid-date)
   :street_number (s/maybe hs/NaturalNumber)
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

(defn time-to-string [keyword map]
  (time/to-string (keyword map)))

(defn times-to-strings [map]
  (assoc map
         :start-time (time-to-string :start-time map)
         :end-time   (time-to-string :end-time map)))

(defn string-categories->keyword-categories [db-quest]
  (assoc db-quest :categories
         (into [] (map keyword (:categories db-quest)))))

(defn db-quest->api-quest [db-quest]
  (-> db-quest
      (location-flat->location-structure)
      (times-to-strings)
      (string-categories->keyword-categories)))

(def db-quest->api-quest-coercer
  (stc/coercer hs/Quest
               {hs/Quest db-quest->api-quest}))

(defn add-quest [{:keys [quest user]}]
  (log/info user)
  (let [quest-with-owner (assoc quest :owner (:id user))
        quest-no-organiser-participates (dissoc quest-with-owner :organiser-participates)
        quest-to-db (api-quest->db-quest-coercer quest-no-organiser-participates)
        quest-id (:id (db/add-unmoderated-quest! quest-to-db))
        quest-from-db (db/get-quest-by-id {:id quest-id})
        quest-to-api (try
                       (pp/pprint quest-from-db)
                       (db-quest->api-quest-coercer quest-from-db)
                       (catch Exception e
                         (log/error e)))]
    quest-to-api))

(defn get-quest [{{id :id} :params}]
  (ok (db/get-quest-by-id {:id id})))

(defn join-quest [params]
  (created ""))
