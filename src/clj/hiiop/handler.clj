(ns hiiop.handler
  (:require [compojure.core :refer [routes wrap-routes]]
            [hiiop.layout :refer [error-page]]
            [hiiop.routes.pages :as pages]
            [hiiop.routes.services :refer [service-routes]]
            [compojure.route :as route]
            [hiiop.env :refer [defaults]]
            [hiiop.config :refer [env]]
            [mount.core :as mount]
            [mount.core :refer [defstate start]]
            [hiiop.middleware :refer [wrap-base wrap-csrf wrap-formats wrap-simple-auth]]
            [taoensso.timbre :as log]))

(mount/defstate init-app
  :start ((or (:init defaults) identity))
  :stop  ((or (:stop defaults) identity)))

(defstate wrap-dev-auth
  :start (partial wrap-simple-auth (:http-simple-credentials env)))
(start #'env)
(start #'wrap-dev-auth)

(def app-routes
  (routes
   (-> #'pages/ring-handler
       (wrap-routes wrap-formats)
       (wrap-dev-auth))
   #'service-routes
   (route/not-found
    (:body
     (error-page
      {:status 404
       :title "Hupsis! Sivua ei löytynyt."
       :message (str
                 "<p class\"opux-content\">"
                 "<h2 class=\"opux-content opux-centered\">"
                 "Hupsis! Sivua ei löytynyt."
                 "</h2>"
                 "</p>")})))))

(defn app [] (wrap-base #'app-routes))
