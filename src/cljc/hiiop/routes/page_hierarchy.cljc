(ns hiiop.routes.page-hierarchy
  (:require [bidi.verbose :refer [branch param leaf]]
            [bidi.bidi :refer [path-for]]))

(def hierarchy
  (branch
   "/"
   (leaf "" :index)
   (leaf "kirjaudu" :login)
   (branch
    "tehtavat/"
    (leaf "" :browse-quests)
    (leaf "luo" :create-quest)
    (branch
     "muokkaa/" (param :quest-id)
     (leaf "" :edit-quest)))))
