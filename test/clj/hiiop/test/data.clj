(ns hiiop.test.data
  (:require [hiiop.time :as time]
            [hiiop.test.util :refer [hash-password]]))

(defn today-at-noon []
  (hiiop.time/time-to (time/now) 12 00))

(defn today-at-six []
  (hiiop.time/time-to (time/now) 18 00))

(defn add-coordinates [in to]
  (let [coordinates {:latitude "60.1928885"
                     :longitude "24.938693000000058"}]
    (if (keyword? to)
      (assoc in to coordinates)
      (conj in coordinates))))

(defn location
  ([coordinates-to]
   (add-coordinates
    {:street-number 3
     :street "Raittipellontie"
     :town "Kolari"
     :postal-code "95900"
     :country "Suomi"
     :google-maps-url "https://maps.google.com/?q=Raittipellontie+3,+95900+Kolari,+Suomi&ftid=0x45d38d5d73e6a2bf:0xacc1db81b0f0ef42"
     :google-place-id "ChIJC3Oo44UJkkYR8Br7zHxjnTs"}
    coordinates-to))
  ([] (location nil)))

(defn add-location [quest {:keys [location-to coordinates-to]}]
  (if (keyword? location-to)
    (assoc quest location-to (location coordinates-to))
    (conj quest (location coordinates-to))))

(defn test-quest
  ([{:keys [use-date-string location-to coordinates-to]}]
   (let [start-time (today-at-noon)
         end-time (today-at-six)]
     (println use-date-string location-to)
     (add-location
      {:name "Nälkäkeräys"
       :start-time (if use-date-string (time/to-string start-time) start-time)
       :end-time (if use-date-string (time/to-string end-time) end-time)
       :categories ["foreign-aid"]
       :unmoderated-description "LOL"
       :max-participants 10
       :hashtags ["#a" "#b" "#c" "#d"]
       :picture nil
       :owner nil
       :is-open true}
      {:location-to location-to
       :coordinates-to coordinates-to })))
  ([] (test-quest {})))

(def test-user
  {:name "Test user"
   :email "test@email.com"
   :pass (hash-password "password#")
   :password "password#"
   :is_active true})
