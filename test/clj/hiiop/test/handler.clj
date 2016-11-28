(ns hiiop.test.handler
  (:require [clojure.test :refer :all]
            [ring.mock.request :refer :all]
            [cheshire.core :refer [generate-string parse-string]]
            [taoensso.timbre :as log]
            [hiiop.handler :refer :all]
            [hiiop.db.core :as db]
            [hiiop.test.test :refer [contains-many?]]))

(deftest test-app
  (testing "main route"
    (let [response ((app) (request :get "/"))]
      (is (= 200 (:status response)))))

  (testing "not-found route"
    (let [response ((app) (request :get "/invalid"))]
      (is (= 404 (:status response)))))

  (testing "language override cookie set"
    (let [response ((app) (request :get "/?lang=sv"))
          set-cookie (last (get-in response [:headers "Set-Cookie"]))
          lang (if set-cookie (last (re-find #"lang=(sv)" set-cookie)))]
      (is (= "sv" lang))))

  (testing "language override cookie send"
    (let [response ((app)
                    (assoc (request :get "/api/v1/config") :cookies {"lang" {:value "sv"}}))
          config (parse-string (slurp (:body response)) true)
          lang (first (:accept-langs config))]
      (is (= "sv" lang)))))

(defn json-post [endpoint body-string]
  (->
    (request :post endpoint)
    (body body-string)
    (content-type "application/json")))

(deftest test-api
  (testing "/api/v1/quest/add"
    (let [quest-to-add {:name                    "Nälkäkeräys"
                        :organisation            {:name "Punainen risti"}
                        :start-time              "2016-12-06T12:00:00+02:00"
                        :end-time                "2016-12-06T18:00:00+02:00"
                        :address                 "Raittipellontie 4"
                        :town                    "Kolari"
                        :categories              ["foreign-aid"]
                        :unmoderated-description "LOL"
                        :max-participants        1
                        :is-open                 true}
          quest-to-add-json (generate-string quest-to-add)
          response ((app) (json-post "/api/v1/quest/add" quest-to-add-json))
          body (slurp (:body response))
          body-map (parse-string body true)]
      (is (= 201 (:status response)))
      (is (> (:id body-map) 0))
      (is (= (:start-time quest-to-add) (:start-time body-map)))
      (is (contains-many?
           body-map
           :id
           :name
           :start-time
           :end-time
           :unmoderated-description
           :address
           :town
           :categories
           :max-participants
           :is-open))
      (db/delete-quest-by-id! {:id (:id body-map)}))
    ))
