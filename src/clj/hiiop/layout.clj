(ns hiiop.layout
  (:require [ring.util.http-response :refer [content-type ok]]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
            [rum.core :as rum]
            [hiiop.config :refer [env asset-path]]
            [hiiop.html :refer [app-structure]]))


(declare ^:dynamic *app-context*)

(defn render
  "renders the HTML template located relative to resources/templates"
  [content & [params]]
  (content-type
   (ok
    (rum/render-html
     (app-structure {:asset-path (asset-path env)
                     :content content
                     :csrf-token *anti-forgery-token*
                     :servlet-context *app-context* })))
    "text/html; charset=utf-8"))

(defn error-page
  "error-details should be a map containing the following keys:
   :status - error status
   :title - error title (optional)
   :message - detailed error message (optional)

   returns a response map with the error page as the body
   and the status specified by the status key"
  [error-details]
  {:status  (:status error-details)
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body    "Error..."})
