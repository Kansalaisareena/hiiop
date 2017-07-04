(ns hiiop.file-upload
  (:require [mount.core :refer [defstate] :as mount]
            [pantomime.mime :as pm]
            [taoensso.timbre :as log]
            [amazonica.aws.s3 :as s3]
            [hiiop.config :refer [env]]
            [schema.core :as s]
            [clojure.java.io :refer [copy]]
            [clj-http.client :as http]
            [image-resizer.core :refer [resize-to-width]]
            [image-resizer.format :refer [as-file]]
            [clojure.java.io :as io]
))

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
  :start
  (let [blog-bucket (:hiiop-blog-bucket env)]
    (log/info "Starting with blog bucket" blog-bucket)
    blog-bucket))

(defstate hiiop-bucket
  :start
  (let [hiiop-bucket (:hiiop-bucket env)]
    (log/info "Starting with hiiop bucket" hiiop-bucket)
    hiiop-bucket))

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
        (throw e))
      (finally (.delete temp-file)))))

(defn upload-picture-to-s3 [id picture-file]
  (let [extension (pm/extension-for-name (:content-type picture-file))
        tempfile-name (:absolutePath (bean (:tempfile picture-file)))
        image-name (str id extension)
        original-key (str "images/" image-name)
        small-key (str "images/small/" image-name)]
    (s3/put-object aws-credentials
                            :bucket-name picture-bucket
                            :key original-key
                            :file (:tempfile picture-file)
                            :metadata
                            {:content-type (:content-type picture-file)})
    (let [small-image (as-file
                       (resize-to-width (io/file tempfile-name) 450)
                       (str tempfile-name ".small" extension) :verbatim)         ]
      (try
        (s3/put-object aws-credentials
                            :bucket-name picture-bucket
                            :key small-key
                            :file small-image
                            :metadata
                            {:content-type (:content-type picture-file)})
        (finally (io/delete-file small-image))))
    (str bucket-base-url "/" original-key)))

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

