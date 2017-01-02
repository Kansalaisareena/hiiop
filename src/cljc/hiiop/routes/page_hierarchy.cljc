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
   (leaf "tehtavat" :browse-quests)

   (branch
     "tehtavat/"
     (leaf "hyvaksynta" :moderate))

  (branch
    "tehtavat/" (param :quest-id)
    (leaf "" :quest)

    (branch
     "/peruuta/" (param :member-id)
     (leaf "" :part-quest-party))

    (branch
     "/salainen/" (param :secret-party)
     (leaf "" :secret-quest))

    (branch
     "/muokkaa/"
     (leaf "" :edit-quest)))

   ))
