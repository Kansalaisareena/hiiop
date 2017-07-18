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
            [hiiop.url :refer [image-url-to-small-url]]))

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

;; run function that takes a resized image, delete it afterward
(defn with-resized-image [picture-file extension width f]
  (let [small-image (try (as-file (resize-to-width (io/file picture-file) width)
                                  (str picture-file ".small" extension))
                         (catch Exception e
                           picture-file))]
    (try
      (f small-image)
      (finally (io/delete-file small-image)))))

(defn- resize-and-upload-picture [picture-file content-type width bucket key]
  (let [extension (pm/extension-for-name content-type)
        small-key (image-url-to-small-url key)]
    (s3/put-object aws-credentials
                            :bucket-name bucket
                            :key key
                            :file picture-file
                            :metadata
                            {:content-type content-type})
    (with-resized-image picture-file (pm/extension-for-name content-type) width
      (fn [small-picture]
        (s3/put-object aws-credentials
                            :bucket-name bucket
                            :key small-key
                            :file small-picture
                            :metadata
                            {:content-type content-type})))))

(defn upload-picture-to-s3 [id picture-file]
  (let [extension (pm/extension-for-name (:content-type picture-file))
        tempfile-name (:absolutePath (bean (:tempfile picture-file)))
        image-name (str id extension)
        original-key (str "images/" image-name)]
    (resize-and-upload-picture tempfile-name (:content-type picture-file) 312 picture-bucket original-key)
    (str bucket-base-url "/" original-key)))

(defn- is-image [filename] (not  (empty?(re-find #"(jpg|jpeg|png|gif|JPG|JPEG|PNG|GIF)$" filename))))

(defn get-and-upload-asset-to-s3 [from to]
  (with-temp-file
    (fn [temp]
      (let [response (http/get from {:as :stream})
            content-type (get-in response [:headers "Content-Type"])
            data-stream (:body response)]
        (copy data-stream temp)
        (if (is-image from)
          (resize-and-upload-picture temp content-type 312 blog-bucket to)
          (s3/put-object aws-credentials
                         :bucket-name blog-bucket
                         :key to
                         :file temp
                         :metadata {:content-type content-type}))))))

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

