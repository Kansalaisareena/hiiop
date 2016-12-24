(ns hiiop.schema
  (:require [clojure.string :as str]
            [schema.core :as s :include-macros true]
            [schema.coerce :as sc :include-macros true]
            #?(:cljs [schema.utils :refer [ValidationError]])
            #?(:cljs [schema.core :refer [Constrained]])
            [schema-tools.core :as st]
            [schema-tools.coerce :as stc]
            [taoensso.timbre :as log]
            [hiiop.time :as time])
  #?(:clj (:import [schema.utils ValidationError]
                   [schema.core Constrained])))

(def Email
  (s/constrained
   #"(^[a-zA-Z0-9._+-]+@[^@.]+\.[^@.]+)$"
   #(= % %) :errors.email.not-valid))

(def Password s/Str)

(def Phone
  (s/constrained
   #"^(([^\.\-\,a-wy-z]([\(]?(\+|[x])?\d+[\)]?)?[\s\.\-\,]?([\(]?\d+[\)]?)?[\s\.\-\,]?(\d+[\s\.\-\,]?)+[^\.\-\,a-z])|((\+|[x])?\d+))$"
   (fn [val] (not (nil? val))) :errors.phone.not-valid))

(def Agreement
  (s/constrained
   s/Bool #(= true %) :errors.quest.please-agree))

(def DateTime
  (s/constrained
   #"^([\+-]?\d{4}(?!\d{2}\b))((-?)((0[1-9]|1[0-2])(\3([12]\d|0[1-9]|3[01]))?|W([0-4]\d|5[0-2])(-?[1-7])?|(00[1-9]|0[1-9]\d|[12]\d{2}|3([0-5]\d|6[1-6])))([T\s]((([01]\d|2[0-3])((:?)[0-5]\d)?|24\:?00)([\.,]\d+(?!:))?)?(\17[0-5]\d([\.,]\d+)?)?([zZ]|([\+-])([01]\d|2[0-3]):?([0-5]\d)?)?)?)?$"
   time/from-string :errors.not-valid-date))

(def NaturalNumber
  "A schema for natural number integer"
  (s/constrained s/Int (comp not neg?) :errors.number.not-greater-than-one))

(def NPlus
  "A schema for natural number integer greater than one"
  (s/constrained NaturalNumber (partial < 0) :errors.number.greater-than-zero))

(def NonEmptyString
  "A schema for non-empty string"
  (s/constrained s/Str (fn [val] (> (count val) 0)) :errors.non-empty-string))

(def Organisation
  "Organisation"
  {(s/optional-key :name) (s/maybe s/Str)
   (s/optional-key :description) (s/maybe s/Str)})

(def User
  "Registered or virtual user"
  {:email Email
   (s/optional-key :name) (s/maybe s/Str)
   :id s/Uuid
   (s/optional-key :organisation) (s/maybe Organisation)
   (s/optional-key :phone) (s/maybe Phone)
   :moderator s/Bool
   :active s/Bool})

(def NewGuestUser
  "New guest"
  (st/dissoc User
             :id
             :organisation
             :moderator
             :active))

(defn new-empty-registration-info []
  {:name "" :email ""})

(def RegistrationInfo
  "Initial registration information with only email and name"
  {:email Email
   :name s/Str})

(def UserActivation
  "Email, password and password token"
  {:email Email
   :password Password
   :confirm-password Password
   :token s/Uuid})

(defn new-empty-activation-info []
  {:token nil
   :email ""
   :password ""
   :confirm-password ""})

(def UserCredentials
  "Email and password"
  {:email Email
   :password Password})

(def Category
  (s/enum
   :kids-and-youngsters
   :elderly
   :disabilities
   :peer-support
   :inequality
   :foreign-aid
   :culture
   :equality
   :well-being
   :environment))

(def Categories
  (-> [Category]
      (s/constrained not-empty)))

(def Hashtag #"^#[^\\-]+$")

(def Coordinates
  "Latitude and longitude"
  {:latitude s/Str
   :longitude s/Str})

(def Location
  "Location"
  {(s/optional-key :google-place-id) (s/maybe NonEmptyString)
   (s/optional-key :street-number) (s/maybe s/Int)
   (s/optional-key :street) (s/maybe NonEmptyString)
   :town NonEmptyString
   :postal-code NonEmptyString
   :country NonEmptyString
   (s/optional-key :coordinates) (s/maybe Coordinates)
   (s/optional-key :google-maps-url) (s/maybe NonEmptyString)
   })

(def QuestFilter
  "Quest listing filter"
  {:categories [Category]
   :location Location
   :start-time DateTime
   :end-time DateTime})

(defn new-empty-quest-filter []
  {:categories []
   :location {}
   :start-time (time/to-string
                (time/tomorrow-at-noon)
                time/transit-format)
   :end-time (time/to-string
              (time/time-to (time/tomorrow) 23 45)
              time/transit-format)})

(def Quest
  "Quest"
  {:id NaturalNumber
   :name NonEmptyString
   :description NonEmptyString
   :start-time DateTime
   :end-time DateTime
   :location Location
   :max-participants NPlus
   :categories Categories
   (s/optional-key :owner-name) (s/maybe s/Str)
   (s/optional-key :picture-id) (s/maybe s/Str)
   (s/optional-key :picture-url) (s/maybe s/Str)
   (s/optional-key :hashtags) [Hashtag]
   :is-open s/Bool
   :owner s/Uuid
   (s/optional-key :organisation) (s/maybe Organisation)})

(def EditQuest Quest)

(def NewQuest
  (-> EditQuest
      (st/assoc :organiser-participates s/Bool)
      (st/dissoc :id
                 :owner)))

(def Quests
  "Quest list"
  [Quests])

(defn new-empty-quest-signup-info []
  {:name "" :email "" :phone "" :participate-days 1 :agreement false})

;(def UrlLike #"http[s]{0,1}:\/\/.*")

(def Picture
  "Uploaded picture"
  {:id s/Uuid
   (s/optional-key :alias) (s/maybe s/Str)
   :url s/Str})

(defn new-empty-quest []
  {:name ""
   :description ""
   :hashtags []
   :start-time (time/to-string
                (time/tomorrow-at-noon)
                time/transit-format)
   :end-time (time/to-string
              (time/time-to (time/tomorrow) 23 45)
              time/transit-format)
   :picture-id nil
   :picture-url nil
   :location {}
   :max-participants 10
   :categories []
   :is-open true
   :organiser-participates true})

(def QuestSignup
  "Quest signup information"
  {(s/optional-key :name) (s/maybe NonEmptyString)
   :email Email
   (s/optional-key :phone) (s/maybe Phone)
   :agreement Agreement})


(def PartyMember
  "Party member"
  {:id s/Uuid
   :quest-id s/Int
   :user-id s/Uuid
   :days NPlus})

(def NewPartyMember
  "New party member"
  (s/conditional #(not (nil? (:user-id %1)))
                 {(s/optional-key :quest-id) s/Int
                  :user-id s/Uuid
                  :days (st/get-in PartyMember [:days])
                  (s/optional-key :secret-party) (s/maybe s/Uuid)}
                 #(not (nil? (:signup %1)))
                 {:signup QuestSignup
                  :days (st/get-in PartyMember [:days])
                  (s/optional-key :secret-party) (s/maybe s/Uuid)}))

(defn message-from-constrained [^Constrained c]
  (:post-name c))

(defn message-from-validation-error [^ValidationError error or]
  (if error
    (try
      #?(:clj  (.post-name (.schema error))
         :cljs (.-post-name (.-schema error)))
      (catch #?(:clj Exception :cljs js/Error) e
          or))))

(defn either [coercer value]
  (try
    {:--value (coercer value)}
    (catch #?(:clj Exception :cljs js/Error) e
        (let [data (ex-data e)
              schema-error (message-from-constrained (:schema data))
              type-error (:type data)
              found-error (or schema-error type-error :unknown-error)]
          {:--error found-error}))))

(defn category-choice [choice]
  (keyword (str "category." (name choice))))

(defn select-schema-either [schema value]
  (try
    {:--value (st/select-schema value schema sc/json-coercion-matcher)}
    (catch #?(:clj Exception :cljs js/Error) e
        (let [data (ex-data e)
              schema-error (message-from-constrained (:schema data))
              type-error (:type data)
              found-error (or schema-error type-error :unknown-error)]
          (log/error "select-schema-either" e)
          {:--error found-error}))))

(def CfObject
  "Contentful object"
  {:sys s/Any
   :fields s/Any})

(def Locale)
