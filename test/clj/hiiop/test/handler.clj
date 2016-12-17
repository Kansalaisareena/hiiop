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

(defn remove-user-by-email [user]
  (try
    (db/delete-user-by-email! user)
    (catch Exception e
      ;;(log/error e)
      )))

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

  (testing "api/v1/users/register"

    (testing "with valid info"
      (let [app-with-session (app)
            register-request (json-post
                              "/api/v1/users/register"
                              {:body-string
                               (generate-string {:email (:email test-user)
                                                 :name (:name test-user)})})
            response (app-with-session register-request)]
        (is (= 201 (:status response)))
        (remove-user-by-email test-user)))

    (testing "with duplicate email"
      (let [app-with-session (app)
            create-resp (hiiop.api-handlers/register
                         {:body-params {:email (:email test-user)}})
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
            (if (= 201 (:status resp))
              (log/error email))))
        (doseq [email invalid-emails]
          (remove-user-by-email {:email email}))
        (remove-user-by-email test-user))))

  (testing "/api/v1/users/validate-token"

    (testing "with valid token"
      (let [app-with-session (app)
            uid (hiiop.api-handlers/register
                 {:body-params {:email (:email test-user)
                                :name (:name test-user)}})
            token (db/get-token-by-user-id {:user_id (sc/string->uuid uid)})
            request (json-post
                     "/api/v1/users/validate-token"
                     {:body-string
                      (generate-string {:token (:token token)})})
            response (app-with-session request)
            body (slurp (:body response))
            body-map (parse-string body true)]
        (is (= 200 (:status response)))
        (is (not (nil? (:token body-map))))
        (is (not (nil? (:expires body-map))))
        (is (= (str uid) (:user-id body-map)))
        (is (= (:email test-user)(:email body-map)))
        (remove-user-by-email test-user)))

    (testing "with invalid token"
      (let [app-with-session (app)
            request (json-post
                     "/api/v1/users/validate-token"
                     {:body-string
                      (generate-string {:token (sc/string->uuid "0c161cc5-1a3b-442f-96c7-8a653140134b")})})
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
          (#(db/delete-quest-by-id! {:id (:id %1)})))
      )))
