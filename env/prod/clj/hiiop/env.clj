(ns hiiop.env
  (:require [taoensso.timbre :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[hiiop started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[hiiop has shut down successfully]=-"))
   :middleware identity})
