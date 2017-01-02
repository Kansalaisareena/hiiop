(ns hiiop.layout
  (:require [ring.util.http-response :refer [content-type ok]]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
            [taoensso.timbre :as log]
            [rum.core :as rum]
            [hiiop.config :refer [env asset-path]]
            [hiiop.html :refer [app-structure]]
            [hiiop.routes.page-hierarchy :refer [hierarchy]]
            [hiiop.translate :refer [tr-with]]))


(declare ^:dynamic *app-context*)

(defn render
  "renders the HTML given"
  [{:keys [context content title scripts] :or [params]}]
  (let [final-content (if content (rum/render-html content) "")]
    (content-type
     (ok
      (rum/render-static-markup
       (app-structure {:context (conj context {:asset-path (asset-path env)})
                       :scripts (or scripts [])
                       :title title
                       :content final-content
                       :csrf-token *anti-forgery-token*
                       :servlet-context *app-context* })))
     "text/html; charset=utf-8")))

(defn error-page
  "error-details should be a map containing the following keys:
   :status - error status
   :title - error title (optional)
   :message - detailed error message (optional)

   returns a response map with the error page as the body
   and the status specified by the status key"
  [error-details]
  (let [tr (or
            (:tr error-details)
            (tr-with [:fi]))]
    {:status  (:status error-details)
     :headers {"Content-Type" "text/html; charset=utf-8"}
     :body
     (rum/render-static-markup
      (app-structure {:context {:tr tr
                                :asset-path (asset-path env)
                                :hierarchy hierarchy}
                      :scripts []
                      :no-script true
                      :title (:title error-details)
                      :content (or (:message error-details) "<p></p>")
                      :csrf-token *anti-forgery-token*
                      :servlet-context *app-context* }))
     }))
