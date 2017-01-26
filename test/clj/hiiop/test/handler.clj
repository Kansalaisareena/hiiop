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
            [hiiop.schema :as hs]
            [hiiop.config :refer [load-env]]
            [hiiop.handler :refer :all]
            [hiiop.db.core :refer [*db*] :as db]
            [hiiop.test.util :refer [contains-many? hash-password json-request]]
            [hiiop.test.data :refer [test-quest test-user]]
            [schema-tools.core :as st]
            [clj-time.core :as t]))

(def activation-token (atom nil))
(def password-reset-token (atom nil))
(def new-quest-args (atom nil))
(def edit-quest-args (atom nil))
(def join-quest-args (atom nil))
(def declined-quest-args (atom nil))
(def accepted-quest-args (atom nil))
(def accepted-private-quest-args (atom nil))
(def deleted-quest-args (atom nil))

(defn receive-token-log-and-update [this message]
  (fn [email token locale]
    (log/info message locale token)
    (reset! this token)))

(defn receive-log-and-put-args-to [this log-message]
  (fn [& args]
    (log/info log-message args)
    (reset! this (vec args))))

(-> (mount/except [#'hiiop.core/http-server
                   #'hiiop.core/repl-server
                   #'hiiop.contentful/contentful-init])
    (mount/swap
     {#'hiiop.mail/send-activation-token-email
      (receive-token-log-and-update
       activation-token "received activation token")

      #'hiiop.mail/send-password-reset-token-email
      (receive-token-log-and-update
       password-reset-token "received password-token")

      #'hiiop.mail/send-new-quest-participant-email
      (receive-log-and-put-args-to
       new-quest-args "received new quest participant")

      #'hiiop.mail/send-edit-quest-email
      (receive-log-and-put-args-to
       edit-quest-args "receive edit quest")

      #'hiiop.mail/send-join-quest-email
      (receive-log-and-put-args-to
       join-quest-args "receive join quest")

      #'hiiop.mail/send-quest-declined-email
      (receive-log-and-put-args-to
       declined-quest-args "receive decline quest")

      #'hiiop.mail/send-quest-accepted-email
      (receive-log-and-put-args-to
       accepted-quest-args "received accepted quest")

      #'hiiop.mail/send-private-quest-accepted-email
      (receive-log-and-put-args-to
       accepted-private-quest-args "received private quest")

      #'hiiop.mail/send-quest-deleted-email
      (receive-log-and-put-args-to
       deleted-quest-args "received delete quest")
      })
    mount/start)

(defn log-it [in id]
  (log/info id in)
  in)

(defn has-status [response status url]
  (is (= status (:status response)) url)
  response)

(defn do-this [to with]
  (with to)
  to)

(defn just-do [_ this]
  (this)
  _)

(defn check [this with]
  (with this)
  this)

(defn remove-user-by-email [user]
  (try
    (db/delete-user-by-email! user)
    (catch Exception e
      ;;(log/error e)
      )))

(def test-user-id (atom nil))

(defn session-cookie-string [set-cookie]
  (log/info "set-cookie" set-cookie)
  (if set-cookie
    (let [session-key (last (re-find #"ring-session=([^;]+)" set-cookie))]
      (str "ring-session=" session-key))
    (throw (ex-info "Failed to find login cookie"
                    {:cookie set-cookie}))))

(defn create-test-user [{:keys [user-data save-id-to read-token-from]}]
  (-> (hiiop.api-handlers/register
       {:email (:email user-data)
        :name "Wat"
        :locale :fi})
      (#(reset! save-id-to %))
      ((fn [id]
         (hiiop.api-handlers/activate
          {:password (:password user-data)
           :token    (schema.coerce/string->uuid @read-token-from)})))))

(defn edit-user [{:keys [login-cookie with new-user id]}]
  (let [url (str "/api/v1/users/" id)]
    (log/info "--------------------------edit-user" new-user)
    (-> (generate-string new-user)
        (#(json-request url
           {:type :put
            :body-string %1
            :cookies login-cookie}))
        (with)
        (has-status 200 url)
        (do-this #(log/info %1))
        (:body)
        (slurp)
        (parse-string true))))

(use-fixtures
  :once
  (fn [f]
    (f)
    (db/delete-user! *db* {:id (sc/string->uuid @test-user-id)})))

(defn login-and-get-cookie [{:keys [with user-data]}]
  (-> (json-request "/api/v1/login"
                    {:type :post
                     :body-string
                     (generate-string
                       {:email (:email user-data)
                        :password (:password user-data)})})
      (with)
      (get-in [:headers "Set-Cookie"])
      (first)
      (session-cookie-string)))

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
      (is (= "sv" lang))))

  (testing "moderation page should not be accessible by anonymous user"
    (let [response ((app) (request :get "/tehtavat/hyvaksynta"))]
      (is (= (:status response) 302))
      (db/delete-user! *db* {:id @test-user-id})))

  (testing "moderation page should not be accessible by normal user"
    (let [current-app (app)
          user-created (create-test-user
                         {:user-data test-user
                          :save-id-to test-user-id
                          :read-token-from activation-token})
          login-cookie (login-and-get-cookie
                         {:with current-app
                          :user-data test-user})
          response (current-app
                     (-> (request :get "/tehtavat/hyvaksynta")
                         (header "cookie" login-cookie)))]
      (is (= (:status response) 302))
      (db/delete-user! *db* {:id @test-user-id})))

  (testing "moderation page should be accessible by moderator"
    (let [current-app (app)
          user-created (create-test-user
                         {:user-data test-user
                          :save-id-to test-user-id
                          :read-token-from activation-token})
          made-moderator (db/make-moderator! {:id (sc/string->uuid @test-user-id)})
          login-cookie (login-and-get-cookie
                         {:with current-app
                          :user-data test-user})
          response (current-app
                     (-> (request :get "/tehtavat/hyvaksynta")
                         (header "cookie" login-cookie)))]
      (is (= (:status response) 200))
      (db/delete-user! *db* {:id @test-user-id})))
  )

(defn get-quest [{:keys [quest-id login-cookie with]}]
  (let [url (str "/api/v1/quests/" quest-id)]
    (-> (json-request url
                      {:type :get
                       :cookies login-cookie})
        (with)
        (has-status 200 url)
        (:body)
        (check #(is (not (= %1 nil))))
        (#(when %1 (slurp %1)))
        (parse-string true)
        (do-this #(pp/pprint %1)))))

(defn add-quest [{:keys [login-cookie with quest organiser-participates]}]
  (let [url "/api/v1/quests/add"]
    (-> quest
        (dissoc :picture
                :owner
                :participant-count)
        (assoc :organiser-participates (or organiser-participates false))
        (generate-string)
        (#(json-request url
                        {:type :post
                         :body-string %1
                         :cookies login-cookie}))
        (with)
        (has-status 201 url)
        (:body)
        (slurp)
        (parse-string true)
        )))

(defn edit-quest [{:keys [login-cookie with quest]}]
  (let [url (str "/api/v1/quests/" (:id quest))]
    (-> (generate-string (dissoc quest :participant-count))
        (#(json-request url
           {:type :put
            :body-string %1
            :cookies login-cookie}))
        (with)
        (has-status 200 url)
        (do-this #(log/info %1))
        (:body)
        (slurp)
        (parse-string true)
        )))

(defn delete-quest [{:keys [login-cookie with quest]}]
  (let [url (str "/api/v1/quests/" (:id quest))]
        (-> (generate-string quest)
            (#(json-request url
                            {:type :delete
                             :body-string %1
                             :cookies login-cookie}))
            (do-this pp/pprint)
        (with)
        (has-status 204 url)
        (do-this #(log/info %1))
        )))

(defn joinable-open-quest? [{:keys [quest-id status with]}]
  (let [url (str "/api/v1/quests/"
                 quest-id
                 "/joinable")]
    (-> (json-request url {:type :get})
        (with)
        (has-status (or status 200) url)
        (do-this #(log/info %1))
        (:body)
        (slurp)
        (parse-string true))))

(defn joinable-secret-quest? [{:keys [quest-id secret-party status with]}]
  (let [url (str "/api/v1/quests/" quest-id
                 "/secret/" secret-party
                 "/joinable")]
    (-> (json-request url {:type :get})
        (with)
        (has-status (or status 200) url)
        (do-this #(log/info %1))
        (:body)
        (slurp)
        (parse-string true))))

(defn join-quest [{:keys [login-cookie
                          user-id
                          quest-id
                          secret-party
                          signup
                          days
                          with
                          status]}]

  (let [url (str "/api/v1/quests/" quest-id "/party")]
    (-> (if user-id
          {:user-id user-id
           :days days}
          {:signup signup
           :days days})
        (#(if secret-party
            (assoc %1 :secret-party secret-party)
            %1))
        (generate-string)
        (#(identity {:type :post
                     :body-string %1}))
        (#(if login-cookie
            (assoc %1 :cookies login-cookie)
            %1))
        (#(json-request url
                        %1))
        (with)
        (has-status (or status 201) url)
        (:body)
        (check #(is (not (= %1 nil))))
        (#(when %1 (slurp %1)))
        (parse-string true)
        (do-this #(pp/pprint %1)))))

(defn get-party-members [{:keys [with quest-id login-cookie status]}]
  (let [url (str "/api/v1/quests/" quest-id "/party")]
    (-> (json-request url
         {:type :get
          :cookies login-cookie})
        (with)
        (has-status (or status 200) url)
        (:body)
        (check #(is (not (= %1 nil))))
        (#(when %1 (slurp %1)))
        (parse-string true)
        (do-this #(pp/pprint %1)))))

(defn get-party-member-info [{:keys [with quest-id login-cookie status]}]
  (let [url (str "/api/v1/quests/" quest-id "/get-member-info")]
    (-> (json-request url
         {:type :get
          :cookies login-cookie})
        (with)
        (has-status (or status 200) url)
        (:body)
        (check #(is (not (= %1 nil))))
        (#(when %1 (slurp %1)))
        (parse-string true)
        (do-this #(pp/pprint %1)))))

(defn remove-party-member [{:keys [with quest-id member-id login-cookie status]}]
  (let [url (str "/api/v1/quests/" quest-id "/party/" member-id)]
    (-> (json-request url
         {:type :delete
          :cookies login-cookie})
        (with)
        (has-status (or status 204) url))))

(defn get-moderated-quests [{:keys [with]}]
  (let [url (str "/api/v1/quests/moderated")]
    (-> (json-request url
         {:type :get})
        (with)
        (has-status 200 url)
        (do-this pp/pprint)
        (:body)
        (check #(is (not (= %1 nil))))
        (#(when %1 (slurp %1)))
        (parse-string true)
        (do-this pp/pprint))))

(defn get-user-quests [{:keys [with login-cookie]}]
  (let [url (str "/api/v1/quests/user")]
    (-> (json-request url
                      {:type :get
                       :cookies login-cookie})
        (with)
        (has-status 200 url)
        (:body)
        (check #(is (not (nil? %1))))
        (#(when %1 (slurp %1)))
        (parse-string true)
        (do-this pp/pprint))))

(defn accept-quest [{:keys [with login-cookie quest-id]}]
  (let [url (str "/api/v1/quests/" quest-id "/moderate-accept")]
    (-> (json-request url
                      {:type :post
                       :cookies login-cookie})
        (with)
        (has-status 200 url)
        (:body)
        (check #(is (not (nil? %1))))
        (#(when %1 (slurp %1)))
        (parse-string true)
        (do-this pp/pprint))))

(defn reject-quest [{:keys [with login-cookie quest-id message]}]
  (let [url (str "/api/v1/quests/" quest-id "/moderate-reject")]
    (-> (json-request url
                      {:type :post
                       :body-string (generate-string {:message message})
                       :cookies login-cookie})
        (with)
        (has-status 200 url)
        (:body)
        (check #(is (not (nil? %1))))
        (#(when %1 (slurp %1)))
        (parse-string true)
        (do-this pp/pprint))))

(defn get-moderated-quests [{:keys [with]}]
  (let [url (str "/api/v1/quests/moderated")]
    (-> (json-request url
                      {:type :get})
        (with)
        (has-status 200 url)
        (:body)
        (check #(is (not (= %1 nil))))
        (#(when %1 (slurp %1)))
        (parse-string true)
        (do-this #(pp/pprint %1)))))

(defn request-reset-password [{:keys [with email]}]
  (log/info "request-reset-password" email)
  (let [url (str "/api/v1/users/reset-password")]
    (-> (json-request url
                      {:type :post
                       :body-string (generate-string email)})
        (with)
        (has-status 200 url)
        (:body)
        (check #(is (= %1 nil)))
        (#(when %1 (slurp %1)))
        (#(when %1 (parse-string %1 true)))
        (do-this #(pp/pprint %1)))))

(defn change-password [{:keys [with token password]}]
  (let [url (str "/api/v1/users/change-password")]
    (-> (json-request url
                      {:type :post
                       :body-string (generate-string
                                     {:token token
                                      :password password})})
        (with)
        (has-status 200 url)
        (:body)
        (check #(is (= %1 nil)))
        (#(when %1 (slurp %1)))
        (#(when %1 (parse-string %1 true)))
        (do-this #(pp/pprint %1)))))

(defn get-profile [{:keys [with user-id private login-cookie status is-nil]}]
  (let [url (str "/api/v1/users/" (when private "private/") user-id)]
    (-> (json-request url
                      {:type :get
                       :cookies login-cookie})
        (with)
        (has-status (or status 200) url)
        (:body)
        (check #(is (if is-nil
                      (nil? %1)
                      (not (nil? %1)))))
        (#(when %1 (slurp %1)))
        (#(when %1 (parse-string %1 true)))
        (do-this #(pp/pprint %1)))))

(deftest test-api

  (testing "api/v1/users/register"

    (testing "with valid info"
      (let [current-app (app)
            unique-email "unique.email@example.com"]
        (-> (json-request
             "/api/v1/users/register"
             {:type :post
              :body-string
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
            register-request (json-request
                              "/api/v1/users/register"
                              {:type :post
                               :body-string
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
          (let [register-request (json-request
                                  "/api/v1/users/register"
                                  {:type :post
                                   :body-string
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
            request (json-request
                     "/api/v1/users/validate-token"
                     {:type :post
                      :body-string
                      (generate-string {:token @activation-token})})
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
            request (json-request
                     "/api/v1/users/validate-token"
                     {:type :post
                      :body-string
                      (generate-string {:token
                                        (sc/string->uuid
                                         "0c161cc5-1a3b-442f-96c7-8a653140134b")})})
            response (app-with-session request)]
        (is (= 400 (:status response)))))
    )

  (testing "PUT /api/v1/users/:id"
    (let [current-app (app)
          user-created (create-test-user
                        {:user-data test-user
                         :save-id-to test-user-id
                         :read-token-from activation-token})
          login-cookie (login-and-get-cookie
                        {:with current-app
                         :user-data test-user})
          edit-response (edit-user {:login-cookie login-cookie
                                  :id @test-user-id
                                  :with current-app
                                  :new-user
                                  {:name "Edited User"
                                   :phone "+358 1234567"
                                   :locale :sv}})]
      (is (= "Edited User" (:name  edit-response)))
      (db/delete-user! *db* {:id (sc/string->uuid @test-user-id)})))

  (testing "/api/v1/quests/add"
    (let [current-app (app)
          user-created (create-test-user
                        {:user-data test-user
                         :save-id-to test-user-id
                         :read-token-from activation-token})
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
      (-> (add-quest
           {:with current-app
            :quest quest-to-add
            :login-cookie login-cookie})
          (check #(is (> (:id %1) 0)))
          (check #(is (= (:start-time quest-to-add) (:start-time %1))))
          (do-this pp/pprint)
          (#(db/delete-quest-by-id! {:id (:id %1)}))
          (just-do #(db/delete-user! *db* {:id (sc/string->uuid @test-user-id)})))
      ))

  (testing "/api/v1/quests/add with nil organisation"
    (let [current-app (app)
          user-created (create-test-user
                        {:user-data test-user
                         :save-id-to test-user-id
                         :read-token-from activation-token})
          login-cookie (login-and-get-cookie
                        {:with current-app
                         :user-data test-user})
          quest-with-all-data (test-quest
                               {:use-date-string true
                                :location-to :location
                                :coordinates-to :coordinates
                                :organisation-to {:in :organisation
                                                  :name :name
                                                  :description :description}})
          quest-to-add (assoc quest-with-all-data :organisation {:name nil
                                                                 :description nil})]
      (-> (add-quest
             {:with current-app
              :quest quest-to-add
              :login-cookie login-cookie})
          (do-this pp/pprint)
          (check #(is (> (:id %1) 0)))
          (check #(is (nil? (:organisation %1))))
          (do-this pp/pprint)
          (#(db/delete-quest-by-id! {:id (:id %1)}))
          (just-do #(db/delete-user! *db* {:id (sc/string->uuid @test-user-id)})))
      ))

  (testing "PUT /api/v1/quests/:id with unmoderated quest"
    (let [current-app (app)
          user-created (create-test-user
                         {:user-data test-user
                          :save-id-to test-user-id
                         :read-token-from activation-token})
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
      (-> (add-quest
           {:with current-app
            :quest quest-to-add
            :login-cookie login-cookie})
          (assoc :name "WAAT"
                 :description "OMG!")
          (#(edit-quest {:with current-app
                         :quest %1
                         :login-cookie login-cookie}))
          (check #(is (= "WAAT" (:name %1))))
          (check #(is (= "OMG!" (:description %1))))
          (#(db/delete-quest-by-id! {:id (:id %1)}))
          (just-do #(db/delete-user! *db* {:id (sc/string->uuid @test-user-id)})))
      ))

  (testing "PUT /api/v1/quests/:id with moderated quest"
    (let [current-app (app)
          user-created (create-test-user
                         {:user-data test-user
                          :save-id-to test-user-id
                         :read-token-from activation-token})
          made-moderator (db/make-moderator! {:id (sc/string->uuid @test-user-id)})
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
      (-> (add-quest
           {:with current-app
            :quest quest-to-add
            :login-cookie login-cookie})
          (#(accept-quest {:with current-app
                           :quest-id (:id %1)
                           :login-cookie login-cookie}))
          (assoc :name "WAAT"
                 :description "OMG!")
          (#(edit-quest {:with current-app
                         :quest %1
                         :login-cookie login-cookie}))
          (check #(is (= "WAAT" (:name %1))))
          (check #(is (= "OMG!" (:description %1))))
          (#(db/delete-quest-by-id! {:id (:id %1)}))
          (just-do #(db/delete-user! *db* {:id (sc/string->uuid @test-user-id)})))
      ))

  (testing "PUT /api/v1/quests/:id with nil as postal-code"
    (let [current-app (app)
          user-created (create-test-user
                        {:user-data test-user
                         :save-id-to test-user-id
                         :read-token-from activation-token})
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
      (-> (add-quest
           {:with current-app
            :quest quest-to-add
            :login-cookie login-cookie})
          (assoc :name "WAAT"
                 :description "OMG!")
          (#(assoc-in %1 [:location :postal-code] nil))
          (#(edit-quest {:with current-app
                         :quest %1
                         :login-cookie login-cookie}))
          (check #(is (nil? (get-in %1 [:location :postal-code]))))
          (#(db/delete-quest-by-id! {:id (:id %1)}))
          (just-do #(db/delete-user! *db* {:id (sc/string->uuid @test-user-id)})))
      ))

  (testing "PUT /api/v1/quests/:id with without postal-code"
    (let [current-app (app)
          user-created (create-test-user
                        {:user-data test-user
                         :save-id-to test-user-id
                         :read-token-from activation-token})
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
      (-> (add-quest
           {:with current-app
            :quest quest-to-add
            :login-cookie login-cookie})
          (assoc :name "WAAT"
                 :description "OMG!")
          (#(assoc %1 :location (dissoc (:location %1) :postal-code)))
          (#(edit-quest {:with current-app
                         :quest %1
                         :login-cookie login-cookie}))
          (check #(is (nil? (get-in %1 [:location :postal-code]))))
          (#(db/delete-quest-by-id! {:id (:id %1)}))
          (just-do #(db/delete-user! *db* {:id (sc/string->uuid @test-user-id)})))
      ))

  (testing "DELETE /api/v1/quests/:id with unmoderated quest"
    (let [current-app (app)
          user-created (create-test-user
                        {:user-data test-user
                         :save-id-to test-user-id
                         :read-token-from activation-token})
          login-cookie (login-and-get-cookie
                        {:with current-app
                         :user-data test-user})
          quest-to-add (test-quest
                        {:use-date-string true
                         :location-to :location
                         :coordinates-to :coordinates
                         :organisation-to {:in :organisation
                                           :name :name
                                           :description :description}})
          added-quest (add-quest
                       {:with current-app
                        :quest quest-to-add
                        :login-cookie login-cookie})]
      (-> {:response
           (delete-quest {:with current-app
                          :quest added-quest
                          :login-cookie login-cookie})
           :id (:id added-quest)}
          (check #(is (nil? (db/get-moderated-quest-by-id %1))))
          (#(db/delete-quest-by-id! {:id (:id %1)}))
          (just-do #(db/delete-user! *db* {:id (sc/string->uuid @test-user-id)})))
      ))

  (testing "GET api/v1/quests/:id/joinable"
    (let [current-app (app)
          user-created (create-test-user
                         {:user-data test-user
                          :save-id-to test-user-id
                          :read-token-from activation-token})
          made-moderator (db/make-moderator! {:id (sc/string->uuid @test-user-id)})
          login-cookie (login-and-get-cookie
                         {:with current-app
                          :user-data test-user})
          quest-to-add (test-quest
                         {:use-date-string true
                          :location-to :location
                          :coordinates-to :coordinates
                          :organisation-to {:in :organisation
                                            :name :name
                                            :description :description}})
          added-quest (add-quest
                        {:with current-app
                         :quest quest-to-add
                         :login-cookie login-cookie})]

      (testing "unmoderated quest should not be joinable"
        (is (not (joinable-open-quest?
                   {:quest-id (:id added-quest)
                    :with current-app}))))

      (testing "moderated quest should be joinable"
        (accept-quest {:with current-app
                       :quest-id (:id added-quest)
                       :login-cookie login-cookie})
        (is (joinable-open-quest? {:quest-id (:id added-quest)
                                   :with current-app})))

      (testing "quest that ended should not be joinable"
        (edit-quest
          {:login-cookie login-cookie
           :with current-app
           :quest (assoc added-quest
                         :start-time (-> -365
                                         t/days
                                         t/from-now)
                         :end-time (-> -30
                                       t/days
                                       t/from-now))})
        (accept-quest
          {:with current-app
           :quest-id (:id added-quest)
           :login-cookie login-cookie})
        (is (not (joinable-open-quest?
                   {:quest-id (:id added-quest)
                    :with current-app}))))

      ;; Cleanup
      (db/delete-quest-by-id! {:id (:id added-quest)})
      (db/delete-user! {:id (sc/string->uuid @test-user-id)})))

  (testing "DELETE /api/v1/quests/:id with moderated quest"
    (let [current-app (app)
          user-created (create-test-user
                         {:user-data test-user
                          :save-id-to test-user-id
                          :read-token-from activation-token})
          made-moderator (db/make-moderator! {:id (sc/string->uuid @test-user-id)})
          login-cookie (login-and-get-cookie
                         {:with current-app
                          :user-data test-user})
          quest-to-add (test-quest
                         {:use-date-string true
                         :location-to :location
                         :coordinates-to :coordinates
                         :organisation-to {:in :organisation
                                           :name :name
                                           :description :description}})
          added-quest (add-quest
                       {:with current-app
                        :quest quest-to-add
                        :login-cookie login-cookie})
          accepted-quest (accept-quest {:with current-app
                                        :quest-id (:id added-quest)
                                        :login-cookie login-cookie})]
      (-> {:response
           (delete-quest {:with current-app
                          :quest added-quest
                          :login-cookie login-cookie})
           :id (:id added-quest)}
          (check #(is (nil? (db/get-moderated-quest-by-id %1))))
          (#(db/delete-quest-by-id! {:id (:id %1)}))
          (just-do #(db/delete-user! *db* {:id (sc/string->uuid @test-user-id)})))
      ))

    (testing "POST /api/v1/quests/:id/join with QuestSignup"
      (let [current-app (app)
            user-created (create-test-user
                          {:user-data test-user
                           :save-id-to test-user-id
                           :read-token-from activation-token})
            made-moderator (db/make-moderator! {:id @test-user-id})
            login-cookie (login-and-get-cookie
                          {:with current-app
                           :user-data test-user})
            quest-to-add (test-quest
                          {:use-date-string true
                           :location-to :location
                           :coordinates-to :coordinates
                           :organisation-to {:in :organisation
                                             :name :name
                                             :description :description}})
            added-quest (add-quest
                         {:with current-app
                          :quest quest-to-add
                          :login-cookie login-cookie})
            accepted-quest (accept-quest {:with current-app
                                          :quest-id (:id added-quest)
                                          :login-cookie login-cookie})]
        (-> (:id added-quest)
            (#(join-quest {:quest-id %1
                           :days 1
                           :signup
                           {:name "Erkki Esimerkki"
                            :email "erkki@esimerkki.fi"
                            :agreement true}
                           :with current-app
                           }))
            (check #(is (not (nil? %1))))
            (#(assoc %1 :member-id (schema.coerce/string->uuid (:member-id %1))))
            (#(assoc %1 :user-id (schema.coerce/string->uuid (:user-id %1))))
            (check #(s/validate hs/PartyMember %1))
            (just-do #(db/delete-quest-by-id! {:id (:id added-quest)}))
            (just-do #(db/delete-user! {:id (sc/string->uuid @test-user-id)}))
            (just-do #(db/delete-user-by-email! {:email "erkki@esimerkki.fi"})))
        ))

  (testing "POST /api/v1/quests/:id/join with existing user"
    (let [current-app (app)
          user-created (create-test-user
                        {:user-data test-user
                         :save-id-to test-user-id
                         :read-token-from activation-token})
          made-moderator (db/make-moderator! {:id @test-user-id})
          login-cookie (login-and-get-cookie
                        {:with current-app
                         :user-data test-user})
          quest-to-add (test-quest
                        {:use-date-string true
                         :location-to :location
                         :coordinates-to :coordinates
                         :organisation-to {:in :organisation
                                           :name :name
                                           :description :description}})
          added-quest (add-quest
                       {:with current-app
                        :quest quest-to-add
                        :login-cookie login-cookie})
          accepted-quest (accept-quest {:with current-app
                                        :quest-id (:id added-quest)
                                        :login-cookie login-cookie})]
      (-> (:id added-quest)
          (#(join-quest {:quest-id %1
                         :days 1
                         :user-id @test-user-id
                         :with current-app
                         :login-cookie login-cookie
                         }))
          (check #(is (not (nil? %1))))
          (#(assoc %1 :member-id (schema.coerce/string->uuid (:member-id %1))))
          (#(assoc %1 :user-id (schema.coerce/string->uuid (:user-id %1))))
          (check #(s/validate hs/PartyMember %1))
          (just-do #(db/delete-quest-by-id! {:id (:id added-quest)}))
          (just-do #(db/delete-user! {:id (sc/string->uuid @test-user-id)})))
      ))

  (testing "POST /api/v1/quests/:id/join with existing user"
    (let [current-app (app)
          user-created (create-test-user
                        {:user-data test-user
                         :save-id-to test-user-id
                         :read-token-from activation-token})
          made-moderator (db/make-moderator! {:id @test-user-id})
          login-cookie (login-and-get-cookie
                        {:with current-app
                         :user-data test-user})
          quest-to-add (test-quest
                        {:use-date-string true
                         :location-to :location
                         :coordinates-to :coordinates
                         :organisation-to {:in :organisation
                                           :name :name
                                           :description :description}})
          added-quest (add-quest
                       {:with current-app
                        :quest quest-to-add
                        :login-cookie login-cookie})
          accepted-quest (accept-quest {:with current-app
                                        :quest-id (:id added-quest)
                                        :login-cookie login-cookie})]
      (-> (:id added-quest)
          (#(join-quest {:quest-id %1
                         :days 1
                         :user-id @test-user-id
                         :with current-app
                         :login-cookie login-cookie
                         }))
          (check #(is (not (nil? %1))))
          (check (fn [_] (is (not (nil? @join-quest-args)))))
          (check (fn [_] (is (not (nil? (:quest (first @join-quest-args)))))))
          (check (fn [_] (is (= "test@email.com" (:email (first @join-quest-args))))))
          (just-do #(db/delete-quest-by-id! {:id (:id added-quest)}))
          (just-do #(db/delete-user! {:id (sc/string->uuid @test-user-id)})))
      ))

  (testing "POST /api/v1/quests/:id/join with existing user twice to fail"
    (let [current-app (app)
          user-created (create-test-user
                        {:user-data test-user
                         :save-id-to test-user-id
                         :read-token-from activation-token})
          made-moderator (db/make-moderator! {:id @test-user-id})
          login-cookie (login-and-get-cookie
                        {:with current-app
                         :user-data test-user})
          quest-to-add (test-quest
                        {:use-date-string true
                         :location-to :location
                         :coordinates-to :coordinates
                         :organisation-to {:in :organisation
                                           :name :name
                                           :description :description}})
          added-quest (add-quest
                       {:with current-app
                        :quest quest-to-add
                        :login-cookie login-cookie})
          accepted-quest (accept-quest {:with current-app
                                        :quest-id (:id added-quest)
                                        :login-cookie login-cookie})]
        (-> (join-quest {:quest-id (:id added-quest)
                         :days 1
                         :user-id @test-user-id
                         :with current-app
                         :login-cookie login-cookie
                         })
            (check #(is (nil? (:errors %1))))
            (#(join-quest {:quest-id (:quest-id %1)
                           :days 1
                           :user-id @test-user-id
                           :with current-app
                           :login-cookie login-cookie
                           :status 400
                           }))
            (check #(is (not (nil? (:errors %1)))))
            (just-do #(db/delete-quest-by-id! {:id (:id added-quest)}))
            (just-do #(db/delete-user! {:id (sc/string->uuid @test-user-id)})))
        ))

  (testing "POST /api/v1/quests/:id/join fail when max-participants reached"
    (let [current-app (app)
          user-created (create-test-user
                        {:user-data test-user
                         :save-id-to test-user-id
                         :read-token-from activation-token})
          made-moderator (db/make-moderator! {:id @test-user-id})
          login-cookie (login-and-get-cookie
                        {:with current-app
                         :user-data test-user})
          quest-to-add (test-quest
                        {:use-date-string true
                         :location-to :location
                         :coordinates-to :coordinates
                         :organisation-to {:in :organisation
                                           :name :name
                                           :description :description}})
          added-quest (add-quest
                       {:with current-app
                        :quest (assoc quest-to-add :max-participants 1)
                        :login-cookie login-cookie})
          accepted-quest (accept-quest {:with current-app
                                        :quest-id (:id added-quest)
                                        :login-cookie login-cookie})]
        (-> (join-quest {:quest-id (:id added-quest)
                         :days 1
                         :user-id @test-user-id
                         :with current-app
                         :login-cookie login-cookie
                         })
            (check #(is (nil? (:errors %1))))
            (#(join-quest {:quest-id (:quest-id %1)
                           :days 1
                           :signup
                           {:email "erkki@esimerkki.fi"
                            :agreement true}
                           :with current-app
                           :status 400
                           }))
            (check #(is (not (nil? (:errors %1)))))
            (just-do #(db/delete-quest-by-id! {:id (:id added-quest)}))
            (just-do #(db/delete-user! {:id (sc/string->uuid @test-user-id)}))
            (just-do #(db/delete-user-by-email! {:email "erkki@esimerkki.fi"})))
        ))

  (testing "POST /api/v1/quests/:id/join with secret-party"
    (let [current-app (app)
          user-created (create-test-user
                        {:user-data test-user
                         :save-id-to test-user-id
                         :read-token-from activation-token})
          made-moderator (db/make-moderator! {:id @test-user-id})
          login-cookie (login-and-get-cookie
                        {:with current-app
                         :user-data test-user})
          quest-to-add (test-quest
                        {:use-date-string true
                         :location-to :location
                         :coordinates-to :coordinates
                         :organisation-to {:in :organisation
                                           :name :name
                                           :description :description}})
          added-quest (add-quest
                       {:with current-app
                        :quest (assoc quest-to-add :is-open false)
                        :login-cookie login-cookie})
          accepted-quest (accept-quest {:with current-app
                                        :quest-id (:id added-quest)
                                        :login-cookie login-cookie})
          secret-party (:secret-party
                        (db/get-quest-limitations {:id (:id added-quest)}))]
        (-> (join-quest {:quest-id (:id added-quest)
                         :days 1
                         :user-id @test-user-id
                         :with current-app
                         :login-cookie login-cookie
                         :secret-party secret-party
                         })
            (check #(is (not (nil? %1))))
            (#(assoc %1 :member-id (schema.coerce/string->uuid (:member-id %1))))
            (#(assoc %1 :user-id (schema.coerce/string->uuid (:user-id %1))))
            (check #(s/validate hs/PartyMember %1))
            (just-do #(db/delete-quest-by-id! {:id (:id added-quest)}))
            (just-do #(db/delete-user! {:id (sc/string->uuid @test-user-id)})))
        ))

  (testing "POST /api/v1/quests/:id/join with secret-party to fail"
    (let [current-app (app)
          user-created (create-test-user
                        {:user-data test-user
                         :save-id-to test-user-id
                         :read-token-from activation-token})
          made-moderator (db/make-moderator! {:id @test-user-id})
          login-cookie (login-and-get-cookie
                        {:with current-app
                         :user-data test-user})
          quest-to-add (assoc
                        (test-quest
                        {:use-date-string true
                         :location-to :location
                         :coordinates-to :coordinates
                         :organisation-to {:in :organisation
                                           :name :name
                                           :description :description}})
                        :is-open false)
          added-quest (add-quest
                       {:with current-app
                        :quest quest-to-add
                        :login-cookie login-cookie})
          accepted-quest (accept-quest {:with current-app
                                        :quest-id (:id added-quest)
                                        :login-cookie login-cookie})
          secret-party (:secret-party
                        (db/get-quest-limitations {:id (:id added-quest)}))]
      (-> (join-quest {:quest-id (:id added-quest)
                       :days 1
                       :user-id @test-user-id
                       :with current-app
                       :login-cookie login-cookie
                       :secret-party @test-user-id
                       :status 400
                       })
          (check #(is (not (nil? (:errors %1)))))
          (just-do #(db/delete-quest-by-id! {:id (:id added-quest)}))
          (just-do #(db/delete-user! {:id (sc/string->uuid @test-user-id)})))
      ))

  (testing "POST /api/v1/quests with organiser participates"
    (let [current-app (app)
          user-created (create-test-user
                        {:user-data test-user
                         :save-id-to test-user-id
                         :read-token-from activation-token})
          login-cookie (login-and-get-cookie
                        {:with current-app
                         :user-data test-user})
          quest-to-add (assoc
                        (test-quest
                        {:use-date-string true
                         :location-to :location
                         :coordinates-to :coordinates
                         :organisation-to {:in :organisation
                                           :name :name
                                           :description :description}})
                        :is-open false)
          added-quest (add-quest
                       {:with current-app
                        :quest quest-to-add
                        :login-cookie login-cookie
                        :organiser-participates true})
          ]
      (-> added-quest
          (#(get-party-members {:with current-app
                                :quest-id (:id %1)
                                :login-cookie login-cookie}))
          (check #(is (not (nil? %1))))
          (check #(is (= 1 (count %1))))
          (just-do #(db/delete-quest-by-id! {:id (:id added-quest)}))
          (just-do #(db/delete-user! {:id (sc/string->uuid @test-user-id)})))
      ))

  (testing "GET api/v1/quests/:id/secret/:secret-party/joinable"
    (let [current-app (app)
          user-created (create-test-user
                         {:user-data test-user
                          :save-id-to test-user-id
                          :read-token-from activation-token})
          made-moderator (db/make-moderator! {:id (sc/string->uuid @test-user-id)})
          login-cookie (login-and-get-cookie
                         {:with current-app
                          :user-data test-user})
          quest-to-add (assoc
                         (test-quest
                           {:use-date-string true
                            :location-to :location
                            :coordinates-to :coordinates
                            :organisation-to {:in :organisation
                                              :name :name
                                              :description :description}})
                         :is-open false)
          added-quest (add-quest
                        {:with current-app
                         :quest quest-to-add
                         :login-cookie login-cookie})
          secret-party (:secret-party
                        (db/get-quest-limitations {:id (:id added-quest)}))]

      (testing "unmoderated secret quest should not be joinable"
        (is (not (joinable-secret-quest?
                   {:quest-id (:id added-quest)
                    :secret-party secret-party
                    :with current-app}))))

      (testing "moderated secret quest should be joinable"
        (accept-quest {:with current-app
                       :quest-id (:id added-quest)
                       :login-cookie login-cookie})
        (is (joinable-secret-quest?
              {:quest-id (:id added-quest)
               :secret-party secret-party
               :with current-app})))

      (testing "secret quest with wrong secret token should not be joinable"
        (is (not (joinable-secret-quest?
                   {:quest-id (:id added-quest)
                    :secret-party "0327fcb4-1ddd-4a0c-b103-08dcba7da15d"
                    :with current-app}))))

      (testing "secret quest that ended should not be joinable"
        (edit-quest
          {:login-cookie login-cookie
           :with current-app
           :quest (assoc
                    added-quest
                    :start-time
                    (-> -365
                        t/days
                        t/from-now)
                    :end-time
                    (-> -30
                        t/days
                        t/from-now))})
        (accept-quest
          {:with current-app
           :quest-id (:id added-quest)
           :secret-party secret-party
           :login-cookie login-cookie})
        (is (not (joinable-open-quest?
                   {:quest-id (:id added-quest)
                    :with current-app}))))

      ;; Cleanup
      (db/delete-quest-by-id! {:id (:id added-quest)})
      (db/delete-user! {:id (sc/string->uuid @test-user-id)})))

  (testing "GET /api/v1/quests/:id moderated quest shows correct participant-count"
    (let [current-app (app)
          user-created (create-test-user
                         {:user-data test-user
                          :save-id-to test-user-id
                          :read-token-from activation-token})
          made-moderator (db/make-moderator! {:id @test-user-id})
          login-cookie (login-and-get-cookie
                         {:with current-app
                          :user-data test-user})
          quest-to-add (test-quest
                         {:use-date-string true
                          :location-to :location
                          :coordinates-to :coordinates
                          :organisation-to {:in :organisation
                                            :name :name
                                            :description :description}})
          added-quest (add-quest
                        {:with current-app
                         :quest quest-to-add
                         :login-cookie login-cookie})
          accepted-quest (accept-quest {:with current-app
                                        :quest-id (:id added-quest)
                                        :login-cookie login-cookie})
          joined (join-quest {:quest-id (:id added-quest)
                              :days 1
                              :user-id @test-user-id
                              :with current-app
                              :login-cookie login-cookie})]
      (-> (:id added-quest)
          (#(get-quest {:quest-id %1
                        :login-cookie login-cookie
                        :with current-app}))
          (:participant-count)
          (check #(is (= 1 %1)))
          (just-do #(db/delete-quest-by-id! {:id (:id added-quest)}))
          (just-do #(db/delete-user! {:id (sc/string->uuid @test-user-id)})))))

  (testing "DELETE /api/v1/quests/:quest-id/party/:member-id"
    (let [current-app (app)
          user-created (create-test-user
                        {:user-data test-user
                         :save-id-to test-user-id
                         :read-token-from activation-token})
          login-cookie (login-and-get-cookie
                        {:with current-app
                         :user-data test-user})
          quest-to-add (assoc
                        (test-quest
                        {:use-date-string true
                         :location-to :location
                         :coordinates-to :coordinates
                         :organisation-to {:in :organisation
                                           :name :name
                                           :description :description}})
                        :is-open false)
          added-quest (add-quest
                       {:with current-app
                        :quest quest-to-add
                        :login-cookie login-cookie
                        :organiser-participates true})
          ]
      (-> added-quest
          (#(get-party-member-info {:with current-app
                                    :quest-id (:id %1)
                                    :login-cookie login-cookie}))
          (check #(is (not (nil? %1))))
          (check #(is (= 4 (count %1))))
          (#(remove-party-member {:with current-app
                                  :login-cookie login-cookie
                                  :member-id (:member-id %1)
                                  :quest-id (:id added-quest)
                                  }))
          (just-do #(db/delete-quest-by-id! {:id (:id added-quest)}))
          (just-do #(db/delete-user! {:id (sc/string->uuid @test-user-id)})))
      ))

  (testing "GET /api/v1/quests/user"
    (let [current-app (app)
          moderator (create-test-user
                      {:user-data test-user
                       :save-id-to test-user-id
                       :read-token-from activation-token})
          made-moderator (db/make-moderator! {:id (sc/string->uuid @test-user-id)})
          login-cookie (login-and-get-cookie
                         {:with current-app
                          :user-data test-user})
          quest-to-add (test-quest
                         {:use-date-string true
                          :location-to :location
                          :coordinates-to :coordinates
                          :organisation-to {:in :organisation
                                            :name :name
                                            :description :description}})
          added-quest (add-quest
                        {:with current-app
                         :quest quest-to-add
                         :login-cookie login-cookie})
          quest-id (:id added-quest)
          accepted-quest (accept-quest {:with current-app
                                        :quest-id quest-id
                                        :login-cookie login-cookie})
          normal-user-id (atom nil)
          normal-user-data (assoc test-user :email "normaluser@email.com")
          user-created (create-test-user
                         {:user-data normal-user-data
                          :save-id-to normal-user-id
                          :read-token-from activation-token})
          user-login-cookie (login-and-get-cookie
                              {:with current-app
                               :user-data normal-user-data})
          joined (join-quest {:quest-id quest-id
                              :days 1
                              :user-id @normal-user-id
                              :with current-app
                              :login-cookie user-login-cookie})]
      (-> (get-user-quests {:with current-app
                            :login-cookie user-login-cookie})
          (check #(is (= 1 (count (:attending %1)))))
          (check #(is (= 0 (count (:organizing %1)))))
          (check #(is (= 1 (:participant-count (first (:attending %1)))))))

      ;; add new quest under normal user name
      (let [new-added-quest (add-quest
                              {:with current-app
                               :quest quest-to-add
                               :login-cookie user-login-cookie})
            new-quest-id (:id new-added-quest)]
        (-> (get-user-quests {:with current-app
                              :login-cookie user-login-cookie})
            (check #(is (= 1 (count (:attending %1)))))
            (check #(is (= 1 (count (:organizing %1)))))
            (check #(is (= 1 (:participant-count (first (:attending %1))))))
            (check #(is (= 0 (:participant-count (first (:organizing %1)))))))
        (db/delete-quest-by-id! {:id new-quest-id}))

      (db/delete-quest-by-id! {:id quest-id})
      (db/delete-user! *db* {:id (sc/string->uuid @test-user-id)})
      (db/delete-user! *db* {:id (sc/string->uuid @normal-user-id)})))

  (testing "/api/v1/quests/moderated should be updated after /api/v1/quests/add and /api/v1/quests/:id/moderate"
    (let [current-app (app)
          user-created (create-test-user
                        {:user-data test-user
                         :save-id-to test-user-id
                         :read-token-from activation-token})
          made-moderator (db/make-moderator! {:id @test-user-id})
          login-cookie (login-and-get-cookie
                        {:with current-app
                         :user-data test-user})
          quest-to-add (test-quest
                        {:use-date-string true
                         :location-to :location
                         :coordinates-to :coordinates
                         :organisation-to {:in :organisation
                                           :name :name
                                           :description :description}})
          moderated-quests (get-moderated-quests {:with current-app})]
      (-> moderated-quests
          (check #(is (empty %1)))
          ((fn [_]
            (add-quest
             {:with current-app
              :quest quest-to-add
              :login-cookie login-cookie})))
          (do-this pp/pprint)
          (#(accept-quest {:with current-app
                           :quest-id (:id %1)
                           :login-cookie login-cookie}))
          ((fn [_] (get-moderated-quests {:with current-app})))
          (check #(is (not-empty %1)))
          (#(db/delete-quest-by-id! {:id (:id %1)}))
          (just-do #(db/delete-user! *db* {:id (sc/string->uuid @test-user-id)})))
      ))

    (testing "/api/v1/quests/moderated should not be updated after /api/v1/quests/add and /api/v1/quests/:id/moderate"
    (let [current-app (app)
          user-created (create-test-user
                        {:user-data test-user
                         :save-id-to test-user-id
                         :read-token-from activation-token})
          made-moderator (db/make-moderator! {:id @test-user-id})
          login-cookie (login-and-get-cookie
                        {:with current-app
                         :user-data test-user})
          quest-to-add (test-quest
                        {:use-date-string true
                         :location-to :location
                         :coordinates-to :coordinates
                         :organisation-to {:in :organisation
                                           :name :name
                                           :description :description}})
          moderated-quests (get-moderated-quests {:with current-app})]
      (-> moderated-quests
          (check #(is (empty %1)))
          ((fn [_]
            (add-quest
             {:with current-app
              :quest quest-to-add
              :login-cookie login-cookie})))
          (do-this pp/pprint)
          (#(reject-quest {:with current-app
                           :quest-id (:id %1)
                           :login-cookie login-cookie
                           :message "REJECTED!"}))
          ((fn [_] (get-moderated-quests {:with current-app})))
          (check #(is (empty %1)))
          (#(db/delete-quest-by-id! {:id (:id %1)}))
          (just-do #(db/delete-user! *db* {:id (sc/string->uuid @test-user-id)})))
      ))

  (testing "/api/v1/quests/moderated should not be updated after /api/v1/quests/add and /api/v1/quests/:id/moderate"
    (let [current-app (app)
          user-created (create-test-user
                        {:user-data test-user
                         :save-id-to test-user-id
                         :read-token-from activation-token})
          made-moderator (db/make-moderator! {:id @test-user-id})
          login-cookie (login-and-get-cookie
                        {:with current-app
                         :user-data test-user})
          quest-to-add (test-quest
                        {:use-date-string true
                         :location-to :location
                         :coordinates-to :coordinates
                         :organisation-to {:in :organisation
                                           :name :name
                                           :description :description}})
          moderated-quests (get-moderated-quests {:with current-app})
          reject-email-reset (reset! declined-quest-args nil)]
      (-> moderated-quests
          (check #(is (empty %1)))
          ((fn [_]
            (add-quest
             {:with current-app
              :quest quest-to-add
              :login-cookie login-cookie})))
          (#(reject-quest {:with current-app
                           :quest-id (:id %1)
                           :login-cookie login-cookie
                           :message "REJECTED!"}))
          (check (fn [_] (is (not-empty @declined-quest-args))))
          (#(db/delete-quest-by-id! {:id (:id %1)}))
          (just-do #(db/delete-user! *db* {:id (sc/string->uuid @test-user-id)})))
      ))

  (testing "/api/v1/users/reset-password /api/v1/users/change-password should change password"
    (let [current-app (app)
          user-created (create-test-user
                        {:user-data test-user
                         :save-id-to test-user-id
                         :read-token-from activation-token})]
      (-> {:request-password-reset
           (request-reset-password
            {:with current-app
             :email (:email test-user)})}
          (assoc :token @password-reset-token)
          (assoc :password "LOL1234!")
          (#(assoc %1 :changed-password
                   (change-password {:with current-app
                                     :token (:token %1)
                                     :password (:password %1)})))
          (#(assoc %1 :login
                   (login-and-get-cookie {:with current-app
                                          :user-data
                                          {:email (:email test-user)
                                           :password (:password %1)}})))
          (check #(= (not (nil? (:login %1)))))
          (just-do #(db/delete-user! *db* {:id (sc/string->uuid @test-user-id)})))
      ))

  (testing "getting public user should work"
    (let [current-app (app)
          user-created (create-test-user
                        {:user-data test-user
                         :save-id-to test-user-id
                         :read-token-from activation-token})]
      (-> (get-profile {:with current-app
                        :user-id @test-user-id})
          (#(assoc {} :public-user %1))
          (check #(= (not (nil? (:public-user %1)))))
          (just-do #(db/delete-user! *db* {:id (sc/string->uuid @test-user-id)})))
      ))

  (testing "getting private user should work"
    (let [current-app (app)
          user-created (create-test-user
                        {:user-data test-user
                         :save-id-to test-user-id
                         :read-token-from activation-token})]
      (-> (assoc {}
                 :login
                 (login-and-get-cookie
                  {:with current-app
                   :user-data test-user}))
          (#(assoc %1 :private-user
                   (get-profile {:with current-app
                                 :user-id @test-user-id
                                 :private true
                                 :login-cookie (:login %1)})))
          (check #(= (not (nil? (:private-user %1)))))
          (just-do #(db/delete-user! *db* {:id (sc/string->uuid @test-user-id)})))
      ))

  (testing "getting private user should't work if not logged in"
    (let [current-app (app)
          user-created (create-test-user
                        {:user-data test-user
                         :save-id-to test-user-id
                         :read-token-from activation-token})]
      (-> (assoc {}
                 :private-user
                 (get-profile {:with current-app
                               :user-id @test-user-id
                               :private true
                               :status 401
                               :is-nil true}))
          (just-do #(db/delete-user! *db* {:id (sc/string->uuid @test-user-id)})))
      ))


  (testing "getting private user should't work if not logged in"
    (let [current-app (app)
          user-created (create-test-user
                        {:user-data test-user
                         :save-id-to test-user-id
                         :read-token-from activation-token})
          user2-id (atom nil)
          user2-data (assoc test-user :email "email@foo.bar")
          user2-created (create-test-user
                         {:user-data user2-data
                          :save-id-to user2-id
                          :read-token-from activation-token})]
      (-> (assoc {}
                 :login
                 (login-and-get-cookie
                  {:with current-app
                   :user-data test-user}))
          (#(assoc %1
                   :private-user
                   (get-profile {:with current-app
                                 :user-id @user2-id
                                 :login-cookie (:login %1)
                                 :private true
                                 :status 400})))
          (just-do #(db/delete-user! *db* {:id (sc/string->uuid @test-user-id)}))
          (just-do #(db/delete-user! *db* {:id (sc/string->uuid @user2-id)})))
      ))

  )

