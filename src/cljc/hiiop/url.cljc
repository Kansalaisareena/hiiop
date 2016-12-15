(ns hiiop.url
  (:require [clojure.string :as cs]))

(defn vector-to-map [to-map the-map]
  (if (empty? (take 2 to-map))
    the-map
    (let [rest (drop 2 to-map)
          [key value] (take 2 to-map)]
      (vector-to-map rest (conj the-map {(keyword key) value})))
    ))

(defn query-params [url]
  (-> (cs/split url #"\?")
      (last)
      (cs/split #"#")
      (first)
      (cs/split #"&")
      ((fn [entries]
         (flatten
          (map (fn [entry]
                 (cs/split entry #"="))
               entries))))
      (vector-to-map {})))

