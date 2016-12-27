(ns hiiop.file-upload
  (:require [mount.core :refer [defstate] :as mount]
            [pantomime.mime :as pm]
            [taoensso.timbre :as log]
            [amazonica.aws.s3 :as s3]
            [hiiop.config :refer [env]]
            [schema.core :as s]
            [clojure.java.io :refer [copy]]
            [clj-http.client :as http]))

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

(defstate blog-bucket
  :start (:hiiop-blog-bucket env))

(defstate hiiop-bucket
  :start (:hiiop-bucket env))

(defstate bucket-base-url
  :start
  (let [base-url (:hiiop-pictures-bucket-base-url env)]
    (log/info "Starting with picture base url" base-url)
    base-url))


(defn with-temp-file [f]
  (let [temp-file (java.io.File/createTempFile "pre" ".suff")]
    (try
      (f temp-file)
      (catch Exception e
        (.delete temp-file)
        (throw e)))))


(defn upload-picture-to-s3 [id picture-file]
  (-> (pm/extension-for-name (:content-type picture-file))
      (#(str "images/" id %1))
      ((fn [key]
         (s3/put-object aws-credentials
                        :bucket-name picture-bucket
                        :key key
                        :file (:tempfile picture-file)
                        :metadata
                        {:content-type {:content-type picture-file}})
         key
         ))
      (#(str bucket-base-url "/" %1))
      )
  )

(defn get-and-upload-asset-to-s3 [from to]
  (with-temp-file
    (fn [temp]
      (let [response (http/get from {:as :stream})
            content-type (get-in response [:headers "Content-Type"])
            data-stream (:body response)]
        (copy data-stream temp)
        (s3/put-object aws-credentials
                       :bucket-name blog-bucket
                       :key to
                       :file temp
                       :metadata {:content-type content-type})))))

(defn upload-story-to-s3 [key story-html]
  (with-temp-file
    (fn [temp]
      (spit temp story-html)
      (s3/put-object aws-credentials
                     :bucket-name blog-bucket
                     :key key
                     :file temp
                     :metadata {:content-type "text/html"}))))

(defn upload-page-to-s3 [pagekey story-html]
  (with-temp-file
    (fn [temp]
      (spit temp story-html)
      (s3/put-object aws-credentials
                     :bucket-name blog-bucket
                     :key pagekey
                     :file temp
                     :metadata {:content-type "text/html"}))))

(defstate upload-picture :start upload-picture-to-s3)
(defstate upload-story :start upload-story-to-s3)
(defstate upload-page :start upload-page-to-s3)
(defstate get-and-upload-asset :start get-and-upload-asset-to-s3)

