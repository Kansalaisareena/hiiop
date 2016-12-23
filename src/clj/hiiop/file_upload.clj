(ns hiiop.file-upload
  (:require [mount.core :refer [defstate] :as mount]
            [pantomime.mime :as pm]
            [taoensso.timbre :as log]
            [amazonica.aws.s3 :as s3]
            [hiiop.config :refer [env]]
            [schema.core :as s]))

(defstate aws-credentials
  :start
  (let [keys {:access-key (:aws-access-key-id env)
              :secret-key (:aws-secret-access-key env)}]
    (log/info "Starting with AWS credentials" keys)
    keys))

(defstate picture-bucket
  :start
  (let [bucket (:hiiop-pictures-bucket env)]
    (log/info "Starting with picture bucket" bucket)
    bucket))

(defstate bucket-base-url
  :start
  (let [base-url (:hiiop-pictures-bucket-base-url env)]
    (log/info "Starting with picture base url" base-url)
    base-url))

(defn upload-picture-to-s3 [id picture-file]
  (-> (pm/extension-for-name (:content-type picture-file))
      (#(str "images/" id %1))
      ((fn [key]
         (s3/put-object aws-credentials
                        :bucket-name picture-bucket
                        :key key
                        :file (:tempfile picture-file)
                        :metadata
                        {:content-type (:content-type picture-file)})
         key
         ))
      (#(str bucket-base-url "/" %1))
      )
  )

(defstate upload-picture :start upload-picture-to-s3)
