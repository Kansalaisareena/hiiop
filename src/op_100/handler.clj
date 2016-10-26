(ns op-100.handler
  (:require
   [compojure.route :as route]
   [compojure.handler :as handler]
   [compojure.api.sweet :refer :all]
   [ring.util.http-response :refer :all]
   [schema.core :as s]
   [rum.core :as rum]
   [op-100.app :refer [app-structure]]))

(def api-routes
  (api
   {:swagger
    {:ui "/--help"
     :spec "/swagger.json"
     :data {:info
            {:title "Compojure-api-rum-test"
             :description "Compojure Api example"}
            :tags [{:name "api", :description "some apis"}]}}}

   (context "/api" []
     :tags ["api"])))

(defroutes site-routes
  (GET "/index" []
    (rum/render-html (app-structure ["a" "a" "a"])))
  (route/resources "/")
  (route/not-found "404"))

(def site
  (handler/site site-routes))

(def app
  (routes api-routes site))

