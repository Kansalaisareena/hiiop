(ns hiiop.handler
  (:require [ring.util.response :as res]
            [compojure.core :refer [routes wrap-routes]]
            [compojure.route :as route]
            [mount.core :as mount]
            [mount.core :refer [defstate start]]
            [taoensso.timbre :as log]
            [hiiop.layout :refer [error-page]]
            [hiiop.routes.pages :as pages]
            [hiiop.routes.services :refer [service-routes]]
            [hiiop.env :refer [defaults]]
            [hiiop.config :refer [env]]
            [hiiop.middleware :refer [wrap-base wrap-csrf wrap-formats wrap-simple-auth]]
))

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

(defn wrap-cache-headers [handler]
  (fn [request]
    (let [{:keys [headers] :as response} (handler request)
          headers-with-cache (cond
                               (get headers "Cache-Control")
                               headers

                               (:private response)
                               (assoc headers
                                      "Cache-Control" "private, max-age=0, no-cache"
                                      "Pragma" "no-cache")

                               (not (:private response))
                               (assoc headers
                                      "Cache-Control" "public, max-age=60"))]
      (assoc response :headers headers-with-cache))))

(defn app [] (wrap-cache-headers (wrap-base #'app-routes)))
