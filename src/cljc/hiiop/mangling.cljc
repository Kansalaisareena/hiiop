(ns hiiop.mangling
  (:require [taoensso.timbre :as log]
            [camel-snake-kebab.core :refer [->camelCaseString]]
            [camel-snake-kebab.extras :refer [transform-keys]]))

(defn to-value-and-error [map-value key]
  {key {:value (key map-value) :error nil}})

(defn to-error-value-map [map-value]
  (let [object-keys (keys map-value)
        map-to-value-and-error (partial to-value-and-error map-value)]
    (reduce conj (map map-to-value-and-error object-keys))))

(defn same-keys-with-nils [map-value]
  (reduce conj (map (fn [key] {key nil}) (keys map-value))))

(defn parse-natural-number [number-string]
  (#?(:clj Long/parseLong
      :cljs #(js/parseInt % 10)) number-string))

(def ->keys-camelCase (partial transform-keys ->camelCaseString))
