(ns hiiop.mangling
  (:require [taoensso.timbre :as log]
            [camel-snake-kebab.core :refer [->camelCaseString]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [clojure.string :as cs]))

(defn to-value-and-error [map-value key]
  {key {:value (key map-value) :error nil}})

(defn to-error-value-map [map-value]
  (let [object-keys (keys map-value)
        map-to-value-and-error (partial to-value-and-error map-value)]
    (reduce conj (map map-to-value-and-error object-keys))))

(defn same-keys-with-nils [map-value]
  (reduce conj (map (fn [key] {key nil}) (keys map-value))))

(defn parse-natural-number [number-string]
  (try
    (#?(:clj Long/parseLong
        :cljs #(js/parseInt % 10)) number-string)
    (catch #?(:clj Exception :cljs js/Error) e)))

(def ->keys-camelCase (partial transform-keys ->camelCaseString))

(defn readable-address [{:keys [street street-number town]}]
  (-> (filter #(not (nil? %)) [street street-number town])
      ((fn [address]
         (if (not (empty? address))
           (let [last-dropped (drop-last address)
                 before-last (last last-dropped)
                 all-but-last-two (drop-last last-dropped)]
             (concat all-but-last-two [(str before-last ",") (last address)]))
           [])))
      (#(clojure.string/join " " %1))))

(defn split-and-trim-lines [string]
  (filter not-empty (map cs/trim (cs/split-lines string))))
