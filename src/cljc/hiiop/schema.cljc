(ns hiiop.schema
  (:require [schema.core :as s :include-macros true]
            [schema-tools.core :as st]
            [hiiop.time :as time]))

(def Email #"[^@]+@[^.]+\..+")

(def Password s/Str)

(def DateTime
  (s/constrained
   #"^([\+-]?\d{4}(?!\d{2}\b))((-?)((0[1-9]|1[0-2])(\3([12]\d|0[1-9]|3[01]))?|W([0-4]\d|5[0-2])(-?[1-7])?|(00[1-9]|0[1-9]\d|[12]\d{2}|3([0-5]\d|6[1-6])))([T\s]((([01]\d|2[0-3])((:?)[0-5]\d)?|24\:?00)([\.,]\d+(?!:))?)?(\17[0-5]\d([\.,]\d+)?)?([zZ]|([\+-])([01]\d|2[0-3]):?([0-5]\d)?)?)?)?$"
   time/from-string "Not a valid date"))

(def NaturalNumber
  "A schema for natural number integer"
  (s/constrained s/Int (comp not neg?) "Natural number"))

(def NPlus
  "A schema for natural number integer greater than one"
  (s/constrained NaturalNumber (partial < 0) "Greater than one"))

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
   :email-verified? s/Bool})

(def UserRegistration
  (st/assoc
   (st/dissoc User
              :is-moderator?
              :email-verified?
              :id :organisation)
   :password Password))

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

(def Quest
  "Quest"
  {:id NaturalNumber
   :name s/Str
   (s/optional-key :description) s/Str
   :start-time DateTime
   :end-time DateTime
   :address s/Str
   :town s/Str
   :max-participants NPlus
   :unmoderated-description s/Str
   :categories [Category]
   (s/optional-key :hashtags) [s/Str]
   (s/optional-key :picture-id) s/Uuid
   :is-open s/Bool
   :organiser User
   (s/optional-key :organisation) Organisation
   (s/optional-key :party) [User]})

(def NewQuest
  (st/dissoc Quest
             :id
             :description
             :organiser
             :party))

(def NewPartyMember
  "New party member"
  {:user-id s/Uuid
   :days NPlus})
