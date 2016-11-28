(ns hiiop.config
  (:require [cprop.core :refer [load-config]]
            [cprop.source :as source]
            [mount.core :refer [args defstate]]
            [hiiop.version :refer [version gitref]]
            [hiiop.translate :refer [langs]]))

(defstate env :start (load-config
                       :merge
                       [(args)
                        (source/from-system-props)
                        (source/from-env)
                        {:version version
                         :git-ref gitref
                         :langs langs
                         :time-zone "Europe/Helsinki"}]))

(defn asset-path [{:keys [dev asset-base-url git-ref] :or {dev true}}]
  (if dev
    ""
    (str asset-base-url "/" git-ref)))
