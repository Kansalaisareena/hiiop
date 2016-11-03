(ns hiiop.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[hiiop started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[hiiop has shut down successfully]=-"))
   :middleware identity})
