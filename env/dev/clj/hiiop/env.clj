(ns hiiop.env
  (:require [selmer.parser :as parser]
            [taoensso.timbre :as log]
            [hiiop.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[hiiop started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[hiiop has shut down successfully]=-"))
   :middleware wrap-dev})
