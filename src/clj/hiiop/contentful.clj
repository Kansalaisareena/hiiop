(ns hiiop.contentful
  (:require [clojure.core.async :refer [go]]
            [mount.core :refer [defstate]]
            [rum.core :as rum]
            [clj-http.client :as http]
            [rum.core :refer [render-static-markup]]
            [hiiop.redis :refer [wcar*]]
            [cheshire.core :refer [parse-string]]
            [hiiop.config :refer [env asset-path]]
            [hiiop.translate :refer [default-locale tr-opts]]
            [hiiop.routes.page-hierarchy :refer [hierarchy]]
            [hiiop.contentful-page :refer [contentful-page-structure
                                           story-index-page-structure]]
            [taoensso.carmine :as car]
            [mount.core :refer [defstate]]
            [taoensso.timbre :as log]
            [hiiop.file-upload :refer [upload-story upload-page get-and-upload-asset]]
            [hiiop.db.core :as db]
            [me.raynes.cegdown :as md]
            [hiiop.html :as html]
            [taoensso.tempura :as tempura]
            [rum.core :as rum]))

(def cf-url "https://cdn.contentful.com/")
(defstate entries-url :start (str cf-url "spaces/" (:space-id (:contentful env)) "/entries?access_token="
                                  (:cd-api-key (:contentful env)) "&locale=*"))

(def locales [:fi :sv])

(defn localize-fields [fields locale]
  "Given the :fields part of a multi-locale contentful object, returns
  a localized version. If the localization for the field is not found,
  the default locale one is returned."
  (into {} (for [[k v] fields] [k (get v locale (get v default-locale))])))

(defn localize-fields [fields locale]
  "Given the :fields part of a multi-locale contentful object, returns
  a localized version. If the localization for the field is not found,
  the default locale one is returned."
  (into {} (for [[k v] fields] [k (get v locale (get v default-locale))])))

(defn process-email [{{{emailkey :fi} :emailkey} :fields :as email-object}]
  "Render body text and store email object in redis."
  (let [fi-text (get-in email-object [:fields :leipateksti :fi])
        sv-text (get-in email-object [:fields :leipateksti :sv])]
    (wcar* (car/set (str "email:" emailkey)
                    (-> email-object
                        (assoc-in [:fields :leipateksti-rendered :fi]
                                  (md/to-html fi-text))
                        (assoc-in [:fields :leipateksti-rendered :sv]
                                  (md/to-html sv-text)))))))

(defn render-page [cfobject locale]
  (let [fields (localize-fields (:fields cfobject) locale)]
    (contentful-page-structure
      {:locale locale
       :title (:otsikko fields)
       :content (md/to-html (:leipateksti fields))})))

(defn process-page [cfobject]
  (let [id (get-in cfobject [:sys :id])
        pagekey (get-in cfobject [:fields :pagekey :fi])]
    (log/info "processing page:" pagekey)
    (try
      (doseq [locale locales]
        (->> (render-page cfobject locale)
             (upload-page (str (name locale) "/pages/" pagekey ".html"))))
      (catch Exception e
        (log/info "Exception in processing page " pagekey ":" e)))))

(defn render-story [cfobject locale image-url youtube-id]
  (let [fields (localize-fields (:fields cfobject) locale)]
    (contentful-page-structure
      {:locale locale
       :title (:otsikko fields)
       :content (md/to-html (:leipteksti fields))
       :youtube-id youtube-id
       :image-url image-url})))

(defn process-story [cfobject]
  (let [id (get-in cfobject [:sys :id])
        topic-fi (get-in cfobject [:fields :otsikko :fi])
        topic-sv (get-in cfobject [:fields :otsikko :sv])
        image-id (get-in cfobject [:fields :kuva :fi :sys :id])]
    (log/info "processing story: " id)
    (try
      (doseq [locale locales]
        (let [image-url (if image-id
                          (str "/" (name locale) "/kuvat/" image-id)
                          nil)
              youtube-id (get-in cfobject [:fields :youtubeUrl :fi])]
          (->> (render-story cfobject locale image-url youtube-id)
                 (upload-story (str (name locale) "/blog/" id ".html")))))
      (db/add-or-update-story! {:id id :topic-fi topic-fi :topic-sv topic-sv})
      (catch Exception e
        (log/error e)))))

(defn process-asset [cfobject]
  (let [id (get-in cfobject [:sys :id])
        fi-url (str "http:" (get-in cfobject [:fields :file :fi :url]))
        sv-url (str "http:" (get-in cfobject [:fields :file :sv :url]))]
    (go
      (get-and-upload-asset fi-url (str "fi/kuvat/" id))
      (get-and-upload-asset sv-url (str "sv/kuvat/" id)))))

(def handlers
  {"sahkopostiviesti" process-email
   "sivu" process-page
   "tarina" process-story
   "Asset" process-asset})

(defn process-item [item]
  (let [type (if (= (get-in item [:sys :type]) "Entry")
               (get-in item [:sys :contentType :sys :id])
               (get-in item [:sys :type]))
        handler (handlers type)]
    (when handler
      (handler item))))

(defn get-items [skip]
  (let [response (http/get (str entries-url "&skip=" skip))]
    (when (= 200 (:status response))
      (parse-string (:body response) true))))

(defn get-all-items
  "Fetch all items from contentful space."
  ([] (get-all-items 0 {}))
  ([fetched-count prev-items]
   (let [items-response (get-items fetched-count)
         total (:total items-response)
         new-entries (:items items-response)
         new-assets (get-in items-response [:includes :Asset])
         new-items (concat new-entries new-assets)
         new-fetched-count (+ fetched-count (count new-items))]
     (if (<= total new-fetched-count)
       (concat prev-items new-items)
       (recur new-fetched-count (concat prev-items new-items))))))

(defn- filter-stories [items]
  (filter
    (fn [item]
      (let [type (if (= (get-in item [:sys :type]) "Entry")
                   (get-in item [:sys :contentType :sys :id])
                   (get-in item [:sys :type]))]
        (= type "tarina")))
    items))

(defn- story-list-item-data [cfobject locale]
  (let [fields (localize-fields (:fields cfobject) locale)
        id (get-in cfobject [:sys :id])
        image-id (get-in cfobject [:fields :kuva :fi :sys :id])
        image-url (if image-id
                    (str "/" (name locale) "/kuvat/" image-id)
                    nil)
        youtube-id (get-in cfobject [:fields :youtubeUrl :fi])
        title (:otsikko fields)
        excerpt (:excerpt fields)
        content (md/to-html (:leipteksti fields))]
    {:id id
     :url (str (:hiiop-blog-base-url env) "/"
               (name locale) "/blog/"
               id ".html")
     :image-url image-url
     :youtube-id youtube-id
     :content content
     :excerpt excerpt
     :title title
     :locale locale}))

(defn render-stories-index [stories locale]
  (story-index-page-structure
    {:stories stories
     :locale locale}))

(defn process-stories-indexes []
  (let [stories (filter-stories (get-all-items))]
    (doseq [locale locales]
      (let [stories-data
            (map
              #(story-list-item-data % locale)
              stories)]
        (->> (story-index-page-structure
               {:stories stories-data
                :locale locale})
             (upload-page (str (name locale)
                               "/blog/index.html")))))))


(defn refresh-items [items]
  (doseq [i items]
    (process-item i))
  (process-stories-indexes))

(defn update-all-items []
  "Fetch all items from the contentful and do relevant processing and caching."
  (log/info "updating all items")
  (try (-> (get-all-items)
           (refresh-items))
       (catch Exception e
         (log/info "Updating items failed: " e))))

(defstate contentful-init :start (update-all-items))
