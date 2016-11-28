(ns hiiop.client-pages
  (:require [rum.core :as rum]
            [hiiop.components.quests :as quests]
            [hiiop.context :refer [context]]
            [taoensso.timbre :as log]))

(defn browse-quests-page [params]
  (log/info "browse-quests-page")
  (rum/mount
   (quests/list-quests {:quests ["a" "b" "c" "d"]
                        :context @context})
   (. js/document (getElementById "app"))))

(defn create-quest-page [params]
  (log/info "create-quest-page")
  (rum/mount
   (quests/create {:context @context})
   (. js/document (getElementById "app"))))

(defn edit-quest-page [params]
  (log/info "create-quest-page")
  (rum/mount
   (quests/create {:context @context})
   (. js/document (getElementById "app"))))

(def handlers
  {:index
   browse-quests-page
   :browse-quests
   browse-quests-page
   :create-quest
   create-quest-page
   :edit-quest
   edit-quest-page
   })
