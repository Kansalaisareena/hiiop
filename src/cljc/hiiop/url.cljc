(ns hiiop.url
  (:require [clojure.string :as cs]
            [taoensso.timbre :as log]
            #?(:clj [ring.util.response :refer [redirect]])
            [bidi.bidi :refer [path-for]]
            [hiiop.routes.page-hierarchy :refer [hierarchy]]
            [clojure.string :as str]))

(defn vector-to-map [to-map the-map]
  (if (empty? (take 2 to-map))
    the-map
    (let [rest (drop 2 to-map)
          [key value] (take 2 to-map)]
      (vector-to-map rest (conj the-map {(keyword key) value})))
    ))

(defn image-url-to-small-url [url]
  (let
      [split (str/split url #"/")
       [begin end] (split-at (- (count split) 1) split)]
    (str/join "/" (flatten [begin "small" end]))))

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

(defn redirect-to [{:keys [path-key with-params]}]
  (let [path (apply path-for hierarchy path-key with-params)]
    #?(:cljs
       (set! (.-location js/window)
             path)
       :clj
       (redirect path))))

(defn url-to [base-url leaf-name & args]
  (str base-url
       (apply path-for
              hierarchy
              leaf-name
              args)))

