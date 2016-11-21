(ns hiiop.routes.page-hierarchy
  (:require [bidi.verbose :refer [branch param leaf]]
            [bidi.bidi :refer [path-for]]))

(def hierarchy
  (branch
   "/"
   (leaf "" :index)
   (branch
    "tehtavat/"
    (leaf "" :events-index)
    (leaf "luo" :create-event)
    (branch
     "muokkaa/" (param :event-id)
     (leaf "" :edit-event)))))
