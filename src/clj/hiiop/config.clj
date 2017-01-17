(ns hiiop.config
  (:require [cprop.core :refer [load-config]]
            [cprop.source :as source]
            [mount.core :refer [args defstate]]
            [hiiop.version :refer [version gitref]]
            [hiiop.translate :refer [langs]]))

(defn load-env []
  (load-config
   :merge
   [(args)
    (source/from-system-props)
    (source/from-env)
    {:version version
     :git-ref gitref
     :langs langs
     :time-zone "Europe/Helsinki"}]))

(defstate env :start (load-env))

(defn asset-path [{:keys [dev asset-base-url git-ref] :or {dev true}}]
  (if dev
    (or asset-base-url "")
    (str asset-base-url "/" git-ref)))

(def google-maps-url
  (str
   "https://maps.googleapis.com/maps/api/js?"
   "key=AIzaSyDtGq2TOTrSNAKVFyjAIeGFw317iliTrbc"
   "&libraries=places"
   "&language=" "fi" ;; to normalize the google data
   "&region=FI"))
