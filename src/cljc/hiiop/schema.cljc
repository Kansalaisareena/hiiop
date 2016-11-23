(ns hiiop.schema
  (:require
   [schema.core :as s
    :include-macros true]
   [schema-tools.core :as st]))

(def Email #"[^@]+@[^.]+\..+")

(def Password String)

(def Organisation
  "Organisation"
  {:name s/Str})

(def User
  "Registered or virtual user"
  {:email Email
   :name s/Str
   :id s/Uuid
   (s/optional-key :organisation) Organisation
   :is-moderator? s/Bool
   :email-verified? s/Bool})

(def UserRegistration
  (st/assoc (st/dissoc User :is-moderator? :email-verified? :id :organisation)
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

(def Participant
  {:id s/Uuid
   :user User})

(def Quest
  "Quest"
  {:name s/Str
   (s/optional-key :description) s/Str
   :start-time s/Str
   :end-time s/Str
   :pending-description s/Str
   :categories #{Category}
   :image s/Str
   :is-private? s/Bool
   :organiser User
   (s/optional-key :organisation) Organisation
   (s/optional-key :participants) [Participant]})
