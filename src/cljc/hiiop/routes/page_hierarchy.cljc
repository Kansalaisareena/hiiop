(ns hiiop.routes.page-hierarchy
  (:require [bidi.verbose :refer [branch param leaf]]
            [bidi.bidi :refer [path-for]]))

(def hierarchy
  (branch
   "/"
   (leaf "" :index)
   (leaf "kirjaudu" :login)
   (leaf "rekisteroidy" :register)

   (branch
    "rekisteroidy/" (param :token)
    (leaf "" :activate))

   ;; Profile
   (leaf "kayttaja" :profile)

   ;; Quests
   (branch
    "tehtavat/"
    (leaf "" :browse-quests)
    (leaf "luo" :create-quest)
    ;; add :id

    (branch
     "muokkaa/" (param :quest-id)
     (leaf "" :edit-quest)))))
