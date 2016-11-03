(ns hiiop.routes.home
  (:require [hiiop.layout :as layout]
            [compojure.core :refer [defroutes GET]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [hiiop.html :refer [list-events]]))

(defn home-page []
  (layout/render (list-events ["a" "a" "a"])))

(defroutes home-routes
  (GET "/" []
    (home-page)))
