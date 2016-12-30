(ns hiiop.db.core
  (:require
   [camel-snake-kebab.extras :refer [transform-keys]]
   [camel-snake-kebab.core :refer [->snake_case_keyword ->kebab-case-keyword]]
   [cheshire.core :refer [generate-string parse-string]]
   [clojure.java.jdbc :as jdbc]
   [conman.core :as conman]
   [mount.core :refer [defstate]]
   [buddy.hashers :as hashers]
   [taoensso.timbre :as log]
   [clj-time.coerce :as timec]
   [clj-time.jdbc]
   [hiiop.config :refer [env]]
   [hiiop.time :as time]
   [hiiop.redis :refer [redef-with-cache redef-invalidate-cache]])
  (:import org.postgresql.util.PGobject
           java.sql.Array
           clojure.lang.IPersistentMap
           clojure.lang.IPersistentVector
           [java.sql
            BatchUpdateException
            Date
            Timestamp
            PreparedStatement]))

(defn conman-connect []
  (log/info "Starting with database url" (env :database-url))
  (conman/connect! {:jdbc-url (env :database-url)}))

(defn conman-disconnect [connection]
  (conman/disconnect! connection))

(defstate ^:dynamic *db*
  :start (conman-connect)
  :stop (conman-disconnect *db*))

(conman/bind-connection *db* "sql/queries.sql")

(defn result-one-snake->kebab
  [this result options]
  (->> (hugsql.adapter/result-one this result options)
       (transform-keys ->kebab-case-keyword)))

(defn result-many-snake->kebab
  [this result options]
  (->> (hugsql.adapter/result-many this result options)
       (map #(transform-keys ->kebab-case-keyword %))))

(def ->snake_case_keywords
  (partial transform-keys ->snake_case_keyword))

(defmethod hugsql.core/hugsql-result-fn :1 [sym]
  'hiiop.db.core/result-one-snake->kebab)

(defmethod hugsql.core/hugsql-result-fn :one [sym]
  'hiiop.db.core/result-one-snake->kebab)

(defmethod hugsql.core/hugsql-result-fn :* [sym]
  'hiiop.db.core/result-many-snake->kebab)

(defmethod hugsql.core/hugsql-result-fn :many [sym]
  'hiiop.db.core/result-many-snake->kebab)

(extend-protocol jdbc/IResultSetReadColumn
  Array
  (result-set-read-column [v _ _] (vec (.getArray v)))

  PGobject
  (result-set-read-column [pgobj _metadata _index]
    (let [type  (.getType pgobj)
          value (.getValue pgobj)]
      (case type
        "json"       (parse-string value true)
        "jsonb"      (parse-string value true)
        "citext"     (str value)
        value))))

(extend-protocol jdbc/IResultSetReadColumn
  java.sql.Timestamp
  (result-set-read-column [v _2 _3]
    (let [from-db (timec/from-sql-time v)]
      (time/with-default-time-zone from-db))))

(extend-type java.util.Date
  jdbc/ISQLParameter
  (set-parameter [v ^PreparedStatement stmt ^long idx]
    (.setTimestamp stmt idx (Timestamp. (.getTime v)))))

(defn to-pg-json [value]
  (doto (PGobject.)
    (.setType "jsonb")
    (.setValue (generate-string value))))

(extend-type clojure.lang.IPersistentVector
  jdbc/ISQLParameter
  (set-parameter [v ^java.sql.PreparedStatement stmt ^long idx]
    (let [conn      (.getConnection stmt)
          meta      (.getParameterMetaData stmt)
          type-name (.getParameterTypeName meta idx)]
      (if-let [elem-type (when (= (first type-name) \_) (apply str (rest type-name)))]
        (.setObject stmt idx (.createArrayOf conn elem-type (to-array v)))
        (.setObject stmt idx (to-pg-json v))))))

(extend-protocol jdbc/ISQLValue
  IPersistentMap
  (sql-value [value] (to-pg-json value))
  IPersistentVector
  (sql-value [value] (to-pg-json value)))

(defn check-password [email password]
  "Check wether <password> is a valid password for <email>, return
  true/false."
  (let [hash (:pass (get-password-hash {:email email}))]
    (if-not (nil? hash)
      (hashers/check password hash)
      (hashers/check ; prevent timing attack by checking against "dummy_password"
       "wrong_password"
       "bcrypt+blake2b-512$76eb37a62f605eeb7b172c4ba39fa231$12$4ae537eb38a908819b08c495fb78e3afc7e12e336be0e1a2"))))

(defn get-all-quests-by-owner [{:keys [owner]}]
  (let [moderated (get-moderated-quests-by-owner {:owner owner})
        unmoderated (get-unmoderated-quests-by-owner {:owner owner})]
    (concat moderated unmoderated)))

(redef-with-cache get-all-moderated-quests :all-moderated-quests)
(redef-invalidate-cache add-moderated-quest! :all-moderated-quests)
(redef-invalidate-cache add-unmoderated-quest! :all-moderated-quests)
(redef-invalidate-cache update-quest! :all-moderated-quests)
(redef-invalidate-cache delete-quest-by-id! :all-moderated-quests)
(redef-invalidate-cache join-quest! :all-moderated-quests)
(redef-invalidate-cache moderate-accept-quest! :all-moderated-quests)


