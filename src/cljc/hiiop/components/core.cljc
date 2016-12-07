(ns hiiop.components.core
  (:require [rum.core :as rum]
            [schema.core :as s]
            [schema-tools.core :as st]
            [taoensso.timbre :as log]))

(defn value-and-error-cursors-and-schema [{:keys [for schema errors]}]
  (->> (keys @for)
       (map
        (fn [key]
          (let [key-schema (key schema)
                schema-val (st/schema-value (first (vals (st/select-keys schema [key]))))
                chosen-schema (or key-schema schema-val)]
            {key {:value (rum/cursor-in for [key])
                  :error (rum/cursor-in errors [key])
                  :schema chosen-schema}})))
       (reduce conj)))
