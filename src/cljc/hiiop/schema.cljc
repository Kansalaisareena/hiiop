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

(def Email #"[^@]+@[^.]+\..+")

(def Password s/Str)

(def DateTime
  (s/constrained
   #"^([\+-]?\d{4}(?!\d{2}\b))((-?)((0[1-9]|1[0-2])(\3([12]\d|0[1-9]|3[01]))?|W([0-4]\d|5[0-2])(-?[1-7])?|(00[1-9]|0[1-9]\d|[12]\d{2}|3([0-5]\d|6[1-6])))([T\s]((([01]\d|2[0-3])((:?)[0-5]\d)?|24\:?00)([\.,]\d+(?!:))?)?(\17[0-5]\d([\.,]\d+)?)?([zZ]|([\+-])([01]\d|2[0-3]):?([0-5]\d)?)?)?)?$"
   time/from-string :error.not-valid-date))

(def NaturalNumber
  "A schema for natural number integer"
  (s/constrained s/Int (comp not neg?) :error.natural-number))

(def NPlus
  "A schema for natural number integer greater than one"
  (s/constrained NaturalNumber (partial < 0) :error.greater-than-zero))

(def NonEmptyString
  "A schema for non-empty string"
  (s/constrained s/Str (fn [val] (> (count val) 0)) :error.non-empty-string))

(def Organisation
  "Organisation"
  {:name s/Str
   (s/optional-key :description) s/Str})

(def User
  "Registered or virtual user"
  {:email Email
   :name s/Str
   :id s/Uuid
   (s/optional-key :organisation) Organisation
   :is-moderator? s/Bool
   :is-active s/Bool})

(def UserActivation
  "Email, password and password token"
  {:email Email
   :password Password
   :token s/Uuid})

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

(def Hashtag #"^#[^\\-]+$")

(def Coordinates
  "Latitude and longitude"
  {:latitude s/Str
   :longitude s/Str})

(def Location
  "Location"
  {(s/optional-key :google-place-id) NonEmptyString
   (s/optional-key :street-number) s/Num
   (s/optional-key :street) NonEmptyString
   :town NonEmptyString
   :postal-code NonEmptyString
   :country NonEmptyString
   (s/optional-key :coordinates) Coordinates
   (s/optional-key :google-maps-url) NonEmptyString
   })

(def Quest
  "Quest"
  {:id NaturalNumber
   :name NonEmptyString
   (s/optional-key :description) (s/maybe s/Str)
   :start-time DateTime
   :end-time DateTime
   :location Location
   :max-participants NPlus
   :unmoderated-description NonEmptyString
   :categories [Category]
   :picture-url (s/maybe s/Str)
   (s/optional-key :hashtags) [Hashtag]
   :is-open s/Bool
   :owner s/Uuid
   (s/optional-key :organisation) (s/maybe Organisation)})

(def NewQuest
  (st/assoc
   (st/dissoc Quest
              :id
              :description
              :owner
              :picture-url)
   :organiser-participates s/Bool
   (s/optional-key :picture-id) s/Uuid))

(defn new-empty-quest []
  {:name ""
   :unmoderated-description ""
   :hashtags []
   :start-time (time/to-string
                (time/tomorrow-at-noon)
                time/transit-format)
   :end-time (time/to-string
              (time/time-to (time/tomorrow) 23 45)
              time/transit-format)
   :location {}
   :max-participants 10
   :categories []
   :is-open true
   :organiser-participates true})

(def NewPartyMember
  "New party member"
  {:user-id s/Uuid
   :days NPlus})

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
          {:--error found-error}))))
