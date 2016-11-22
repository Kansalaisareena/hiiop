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


;; (def Event
;;   "Event"
;;   {:name s/Str
;;    (s/optional-key :description) s/Str
;;    (s/date )
;;    :pending-description s/Str
;;    (s/optional-key :organisation) Organisation})

