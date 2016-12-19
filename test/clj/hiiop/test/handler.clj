(ns hiiop.test.handler
  (:require [clojure.test :refer :all]
            [clojure.pprint :as pp]
            [clojure.java.jdbc :as jdbc]
            [mount.core :as mount]
            [ring.mock.request :refer :all]
            [taoensso.timbre :as log]
            [cheshire.core :refer [parse-stream generate-string parse-string]]
            [schema.coerce :as sc]
            [schema.core :as s]
            [hiiop.config :refer [load-env]]
            [hiiop.handler :refer :all]
            [hiiop.db.core :refer [*db*] :as db]
            [hiiop.test.util :refer [contains-many? hash-password json-post]]
            [hiiop.test.data :refer [test-quest test-user]]
            [schema-tools.core :as st]))

(defn remove-user-by-email [user]
  (try
    (db/delete-user-by-email! user)
    (catch Exception e
      ;;(log/error e)
      )))

(def test-user-id (atom nil))
(def email-token (atom nil))

(defn receive-email [email token locale]
  (log/info "receive email token: " token)
  (reset! email-token token))

(use-fixtures
  :once
  (fn [f]
    (-> (mount/except [#'hiiop.core/http-server
                       #'hiiop.core/repl-server])
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

(defn create-test-user [{:keys [user-data save-id-to read-token-from]}]
  (-> (hiiop.api-handlers/register {:email (:email user-data)
                                    :name "Wat"
                                    :locale :fi})
      (#(reset! save-id-to %))
      ((fn [id]
         (hiiop.api-handlers/activate
          {:email    (:email user-data)
           :password (:password user-data)
           :token    @read-token-from})))))

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

  (testing "api/v1/users/register"

    (testing "with valid info"
      (let [current-app (app)
            unique-email "unique.email@example.com"]
        (-> (json-post
             "/api/v1/users/register"
             {:body-string
              (generate-string {:email unique-email
                                :name (:name test-user)})})
            (current-app)
            ((fn [response]
               (is (= 200 (:status response))))))
        (remove-user-by-email {:email unique-email})))

    (testing "with duplicate email"
      (let [app-with-session (app)
            create-resp (hiiop.api-handlers/register
                         {:email (:email test-user)
                          :name (:name test-user)
                          :locale :fi})
            register-request (json-post
                              "/api/v1/users/register"
                              {:body-string
                               (generate-string {:email (:email test-user)
                                                 :name (:name test-user)})})
            response (app-with-session register-request)]
        (is (= 400 (:status response)))
        (remove-user-by-email test-user)))

    (testing "with invalid email"
      (let [app-with-session (app)
            invalid-emails [""
                            "invalidemail"
                            "another-tricky@one"
                            "it'sa.trap@joker.com"
                            "it*sa.trap@joker.com"
                            "it sa.trap@joker.com"
                            "almost@valid.com@butno"]]
        (doseq [email invalid-emails]
          (let [register-request (json-post
                                  "/api/v1/users/register"
                                  {:body-string
                                   (generate-string {:email email
                                                     :name "Test User"})})
                resp (app-with-session register-request)]
            (is (= 400 (:status resp)))
            (if (= 200 (:status resp))
              (log/error email))))
        (doseq [email invalid-emails]
          (remove-user-by-email {:email email})))))

  (testing "/api/v1/users/validate-token"

    (testing "with valid token"
      (let [app-with-session (app)
            uid (hiiop.api-handlers/register
                 {:email (:email test-user)
                  :name (:name test-user)
                  :locale :fi})
            request (json-post
                     "/api/v1/users/validate-token"
                     {:body-string
                      (generate-string {:token @email-token})})
            response (app-with-session request)
            body (slurp (:body response))
            body-map (parse-string body true)]
        (is (= 200 (:status response)))
        (is (= false (empty? (:token body-map))))
        (is (= false (empty? (:expires body-map))))
        (is (= (str uid) (str (:user-id body-map))))
        (is (= (:email test-user) (:email body-map)))
        (remove-user-by-email test-user)))

    (testing "with invalid token"
      (let [app-with-session (app)
            request (json-post
                     "/api/v1/users/validate-token"
                     {:body-string
                      (generate-string {:token
                                        (sc/string->uuid
                                         "0c161cc5-1a3b-442f-96c7-8a653140134b")})})
            response (app-with-session request)]
        (is (= 400 (:status response)))))
    )

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
          (#(db/delete-quest-by-id! {:id (:id %1)}))))))
