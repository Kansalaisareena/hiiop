(ns hiiop.config
  (:require [cprop.core :refer [load-config]]
            [cprop.source :as source]
            [mount.core :refer [args defstate]]
            [hiiop.version :refer [version gitref]]))

(defstate env :start (load-config
                       :merge
                       [(args)
                        (source/from-system-props)
                        (source/from-env)
                        {:version version
                         :git-ref (:heroku-slug-commit (source/from-env))}]))

(defn asset-path [{:keys [dev asset-base-url git-ref] :or {dev true}}]
  (if dev
    ""
    (str asset-base-url "/" git-ref)))
