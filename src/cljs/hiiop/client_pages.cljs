(ns hiiop.client-pages
  (:require [clojure.data :as data]
            [rum.core :as rum]
            [taoensso.timbre :as log]
            [schema.core :as schema]
            [hiiop.components.login :as l]
            [hiiop.components.quests :as quests]
            [hiiop.context :refer [context]]
            [hiiop.mangling :refer [same-keys-with-nils]]
            [hiiop.schema :refer [new-empty-quest NewQuest]]))

(defn login-page [params]
  (log/info "login-page")
  (rum/mount
   (l/login {:context @context})
   (. js/document (getElementById "app"))))

(defn browse-quests-page [params]
  (log/info "browse-quests-page")
  (rum/mount
   (quests/list-quests {:quests ["a" "b" "c" "d"]
                        :context @context})
   (. js/document (getElementById "app"))))

(defn validate-new-quest [old-quest new-quest]
  (let [differences (data/diff old-quest new-quest)
        validated (schema/check NewQuest new-quest)]
    (log/info differences validated)))

(defn create-quest-page [params]
  (let [quest (atom (new-empty-quest))
        errors (atom (same-keys-with-nils @quest))]
    (add-watch quest :quest (fn [key atom old-quest new-quest]
                              (validate-new-quest old-quest new-quest)))
    (rum/mount
     (quests/edit {:context @context
                   :quest quest
                   :errors errors
                   :schema NewQuest})
     (. js/document (getElementById "app")))))

(defn edit-quest-page [params]
  (let [quest (atom (new-empty-quest))
        errors (atom (same-keys-with-nils @quest))]
    (log/info "edit-quest-page" @quest (new-empty-quest))
    (rum/mount
     (quests/edit {:context @context
                   :quest quest})
     (. js/document (getElementById "app")))))

(def handlers
  {:index
   browse-quests-page
   :login
   login-page
   :browse-quests
   browse-quests-page
   :create-quest
   create-quest-page
   :edit-quest
   edit-quest-page
   })
