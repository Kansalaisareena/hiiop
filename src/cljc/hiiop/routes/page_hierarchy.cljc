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

   (leaf "luo-tehtava" :create-quest)

   ;; Quests
   (branch
    "tehtavat/"
    (leaf "" :browse-quests))

    (branch
     "tehtavat/" (param :quest-id)
     (leaf "" :quest)

     (branch
      "/muokkaa/"
      (leaf "" :edit-quest)))
    ))
