(ns hiiop.test.handler
  (:require [clojure.test :refer :all]
            [clojure.pprint :as pp]
            [clojure.java.jdbc :as jdbc]
            [mount.core :as mount]
            [ring.mock.request :refer :all]
            [taoensso.timbre :as log]
            [cheshire.core :refer [generate-string parse-string]]
            [schema.coerce :as sc]
            [schema.core :as s]
            [hiiop.config :refer [load-env]]
            [hiiop.handler :refer :all]
            [hiiop.db.core :refer [*db*] :as db]
            [hiiop.test.util :refer [contains-many? hash-password json-post]]
            [hiiop.test.data :refer [test-quest test-user]]))

(def test-user-id (atom nil))
(def email-token (atom nil))

(defn receive-email [email token session]
  (log/info "receive email token: " token)
  (reset! email-token token))

(use-fixtures
  :once
  (fn [f]
    (mount/start-with {#'hiiop.mail/send-token-email receive-email})
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

(defn create-test-user [{:keys [user-data save-id-to read-token-from]}]
  (-> (hiiop.api-handlers/register {:email (:email user-data)
                                    :locale :fi})
      (#(reset! save-id-to %))
      ((fn [id]
         (hiiop.api-handlers/activate
          {:body-params {:email (:email user-data)
                         :password (:password user-data)
                         :token @read-token-from}})))))

(defn login-and-get-cookie [{:keys [with user-data]}]
  (-> (json-post "/api/v1/login"
                 {:body-string
                  (generate-string
                   {:email (:email user-data)
                    :password (:password user-data)})})
      (with)
      (get-in [:headers "Set-Cookie"])
      (first)
      (session-cookie-string)))

(deftest test-api
  (testing "/api/v1/quests/add"
    (let [current-app (app)
          test-user-response (create-test-user
                              {:user-data test-user
                               :save-id-to test-user-id
                               :read-token-from email-token})
          login-cookie (login-and-get-cookie
                        {:with current-app
                         :user-data test-user})
          quest-to-add (test-quest
                        {:use-date-string true
                         :location-to :location
                         :coordinates-to :coordinates
                         :organisation-to {:in :organisation
                                           :name :name
                                           :description :description}})]
      (-> quest-to-add
          (dissoc :picture
                  :owner)
          (assoc :is-open true
                 :organiser-participates true)
          (generate-string)
          (#(json-post "/api/v1/quests/add"
                       {:body-string %1
                        :cookies login-cookie}))
          (current-app)
          ((fn [add-response]
             (is (= 201 (:status add-response)))
             add-response))
          (:body)
          (slurp)
          (parse-string true)
          ((fn [add-body]
             (is (> (:id add-body) 0))
             add-body))
          ((fn [add-body]
             (is (= (:start-time quest-to-add) (:start-time add-body)))
             add-body))
          ((fn [add-body]
             (pp/pprint add-body)
             add-body))
          (#(db/delete-quest-by-id! {:id (:id %1)})))
      )))
