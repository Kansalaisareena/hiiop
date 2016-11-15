(ns hiiop.handler
  (:require [compojure.core :refer [routes wrap-routes]]
            [hiiop.layout :refer [error-page]]
            [hiiop.routes.home :refer [home-routes authed-routes]]
            [hiiop.routes.services :refer [service-routes]]
            [compojure.route :as route]
            [hiiop.env :refer [defaults]]
            [mount.core :as mount]
            [hiiop.middleware :as middleware]
            [immutant.web.middleware :refer [wrap-session]]))

(mount/defstate init-app
  :start ((or (:init defaults) identity))
  :stop  ((or (:stop defaults) identity)))

(def app-routes
  (routes
   (-> #'home-routes
       (wrap-routes middleware/wrap-csrf)
       (wrap-routes middleware/wrap-formats))
   #'service-routes
   (-> #'authed-routes
       (wrap-routes middleware/wrap-csrf)
       (wrap-routes middleware/wrap-formats)
       (wrap-routes middleware/wrap-restricted))
   (route/not-found
    (:body
     (error-page {:status 404
                  :title "page not found"})))))

(defn app [] (middleware/wrap-base #'app-routes))
