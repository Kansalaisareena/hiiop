(ns hiiop.routes.home
  (:require [hiiop.layout :as layout]
            [compojure.core :refer [defroutes GET]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [hiiop.html :refer [list-events]]))

(defroutes authed-routes
  (GET "/secret" req []
       (layout/render {:tr (:tempura/tr req)
                       :content "asd"})))

(defroutes home-routes
  (GET "/" req []
       (let [tr (:tempura/tr req)]
         (layout/render {:tr tr
                         :content (list-events {:events ["a" "a" "a"]
                                                :tr tr})
                         :title (tr [:frontpage])}))))
