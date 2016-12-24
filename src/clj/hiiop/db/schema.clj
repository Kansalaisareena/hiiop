(ns hiiop.db.schema
  (:require [schema.core :as s]
            [schema-tools.core :as st]
            [hiiop.schema :as hs]
            [hiiop.time :as time]))

(def moderated->unmoderated-db-quest-keys
  {:name                     :unmoderated_name
   :description              :unmoderated_description
   :organisation             :unmoderated_organisation
   :organisation_description :unmoderated_organisation_description
   :hashtags                 :unmoderated_hashtags
   :picture                  :unmoderated_picture})

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

(def DBPartyMember
  {:id s/Uuid
   :quest_id s/Int
   :user_id s/Uuid
   :days hs/NPlus})

(def DBNewPartyMember
  (-> (st/dissoc DBPartyMember :id)
      (st/assoc (s/optional-key :secret_party) (s/maybe s/Uuid))))

(def DBUser
  {:id s/Uuid
   :emal hs/Email
   :name (s/maybe hs/NonEmptyString)
   :phone (s/maybe hs/Phone)
   :moderator s/Bool
   :is_active s/Bool})

(def DBGuestUser
  (st/dissoc DBUser :id :moderator :is_active))
