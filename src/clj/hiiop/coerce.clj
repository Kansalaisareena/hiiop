(ns hiiop.coerce
  (:require [schema.core :as s]
            [schema.coerce :as sc]
            [schema-tools.coerce :as stc]
            [hiiop.db.core :as db]
            [hiiop.db.schema :as dbs]
            [hiiop.schema :as hs]
            [hiiop.time :as time]
            [taoensso.timbre :as log]))

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
          (assoc %1
                 :organisation
                 {:name organisation :description organisation-description})
          %1))))

(def db-quest->api-quest-coercer
  (stc/coercer hs/Quest
               {hs/Quest db-quest->api-quest}))

(def db-unmoderated-quest->api-quest-coercer
  (stc/coercer hs/Quest
               {hs/Quest db-quest->api-quest}))

(defn api-quest->new-unmoderated-db-quest [])

(def api-quest->moderated-db-quest-coercer
  (stc/coercer dbs/ModeratedDBQuest
               {dbs/ModeratedDBQuest api-quest->moderated-db-quest}))

(def new-api-quest->new-moderated-db-quest-coercer
  (stc/coercer dbs/NewModeratedDBQuest
               {dbs/NewModeratedDBQuest api-quest->moderated-db-quest}))

(def new-api-quest->new-unmoderated-db-quest-coercer
  (stc/coercer dbs/NewUnmoderatedDBQuest
               {dbs/NewUnmoderatedDBQuest api-quest->new-unmoderated-db-quest}))

(defn api-party-member->db-party-member
  [{:keys [user-id quest-id days] :as args}]
  (db/->snake_case_keywords args))

(def api-new-member->db-new-member-coercer
  (stc/coercer dbs/DBNewPartyMember
               {dbs/DBNewPartyMember api-party-member->db-party-member}))

(defn db-party-member->api-party-member [party-member]
  party-member)

(def db-party-member->api-party-member-coercer
  (stc/coercer hs/PartyMember
               {hs/PartyMember db-party-member->api-party-member}))

(defn api-quest-signup->db-guest-user [signup]
  (-> signup
      (db/->snake_case_keywords)))

(def api-signup->db-user-coercer
  (stc/coercer hs/NewGuestUser
               {dbs/DBGuestUser api-quest-signup->db-guest-user}))
