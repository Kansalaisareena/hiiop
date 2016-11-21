(ns hiiop.routes.page-hierarchy
  (:require [bidi.verbose :refer [branch param leaf]]))

(defn hierarchy [handlers]
  (branch
   "/"
   (leaf "" (:index handlers))
   (branch
    "tehtavat/"
    (leaf "" (:events-index handlers))
    (leaf "luo" (:create-event handlers))
    (branch
     "muokkaa/" (param :event-id)
     (leaf "" (:edit-event handlers))))))
