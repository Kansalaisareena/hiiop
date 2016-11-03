(ns hiiop.schema
  (:require
   [schema.core :as s
    :include-macros true]))

(def Email #"[^@]+@[^.]+\..+")

(def Organisation
  "Organisation"
  {:name s/Str})

(def User
  "Registered or virtual user"
  {:email Email
   (s/optional-key :name) s/Str
   (s/optional-key :organisation) Organisation
   (s/optional-key :is-moderator?) s/Bool
   :email-verified? s/Bool})

;; (def Event
;;   "Event"
;;   {:name s/Str
;;    (s/optional-key :description) s/Str
;;    (s/date )
;;    :pending-description s/Str
;;    (s/optional-key :organisation) Organisation})

