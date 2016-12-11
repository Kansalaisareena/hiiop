(ns hiiop.test.handler
  (:require [clojure.test :refer :all]
            [ring.mock.request :refer :all]
            [clojure.java.jdbc :as jdbc]
            [taoensso.timbre :as log]
            [cheshire.core :refer [generate-string parse-string]]
            [schema.coerce :as sc]
            [mount.core :as mount]
            [hiiop.config :refer [load-env]]
            [hiiop.handler :refer :all]
            [hiiop.db.core :refer [*db*] :as db]
            [hiiop.test.util :refer [contains-many? hash-password json-post]]
            [hiiop.test.data :refer [test-quest test-user]]
            [schema.core :as s]
            [clojure.pprint :as pp]))

(def test-user-id (atom nil))
(def email-token (atom nil))

(defn receive-email [email token]
  (log/info "receive email token" token)
  (reset! email-token token))

(use-fixtures
  :once
  (fn [f]
    (-> (mount/only
         #{#'hiiop.config/env
           #'hiiop.db.core/*db*
           #'hiiop.mail/send-token-email})
        (mount/swap {#'hiiop.mail/send-token-email receive-email})
        mount/start)
    (f)
    (db/delete-user! *db* {:id (sc/string->uuid @test-user-id)})
    ))

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


(defn session-cookie-string [set-cookie]
  (log/info "set-cookie" set-cookie)
  (let [session-key (last (re-find #"ring-session=([^;]+)" set-cookie))]
    (str "ring-session=" session-key)))

(deftest test-api
  (testing "/api/v1/quests/add"
    (let [app-with-session (app)
          new-test-user-id (hiiop.api-handlers/register
                            {:body-params {:email (:email test-user)}})
          wat (reset! test-user-id new-test-user-id)
          new-email-token @email-token
          activate-response (hiiop.api-handlers/activate
                             {:body-params {:email (:email test-user)
                                            :password (:password test-user)
                                            :token new-email-token}})
          login-request (json-post "/api/v1/login"
                                   {:body-string
                                    (generate-string
                                     {:email (:email test-user)
                                      :password (:password test-user)})})
          login-response (app-with-session login-request)
          set-cookie (first (get-in login-response [:headers "Set-Cookie"]))
          session-cookie (session-cookie-string set-cookie)
          test-data (test-quest
                     {:use-date-string true
                      :location-to :location
                      :coordinates-to :coordinates
                      :organisation-to {:in :organisation
                                        :name :name
                                        :description :description}})
          quest-to-add (assoc
                        (dissoc test-data
                                :picture
                                :owner)
                        :is-open true
                        :organiser-participates true)
          quest-to-add-json (generate-string quest-to-add)
          add-request (json-post "/api/v1/quests/add"
                             {:body-string quest-to-add-json
                              :cookies session-cookie})
          add-response (app-with-session add-request)
          add-body (slurp (:body add-response))
          add-body-map (parse-string add-body true)]
      (is (= 201 (:status add-response)))
      (is (> (:id add-body-map) 0))
      (is (= (:start-time quest-to-add) (:start-time add-body-map)))
      (pp/pprint add-body-map)
      (db/delete-quest-by-id! {:id (:id add-body-map)})
      )))
