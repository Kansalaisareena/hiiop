(ns hiiop.contentful
  (:require [mount.core :refer [defstate]]
            [clj-http.client :as http]
            [hiiop.redis :refer [wcar*]]
            [cheshire.core :refer [parse-string]]
            [hiiop.config :refer [env]]
            [hiiop.translate :refer [default-locale]]
            [taoensso.carmine :as car]
            [mount.core :refer [defstate]]
            [taoensso.timbre :as log]
            [me.raynes.cegdown :as md]))

(def cf-url "https://cdn.contentful.com/")
(defstate items-url :start (str cf-url
                                "spaces/"
                                (:space-id (:contentful env))
                                "/entries?access_token="
                                (:cd-api-key (:contentful env))
                                "&locale=*"))

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

(defn process-page [cfobject]
  "Render page and store into aws."
  nil)

(defn process-story [cfobject]
  "Render story and store into aws"
  nil)

(def handlers
  {"sahkopostiviesti" process-email
   "sivu" process-page
   "tarina" process-story})

(defn process-item [item]
  (let [type (get-in item [:sys :contentType :sys :id])
        handler (handlers type)]
    (when handler
      (handler item))))

(defn get-items [skip]
  (let [response (http/get (str items-url "&skip=" skip))]
    (when (= 200 (:status response))
      (parse-string (:body response) true))))

(defn get-all-items
  "Fetch all items from contentful space."
  ([] (get-all-items 0))
  ([fetched-count]
   (let [items (get-items fetched-count)
         new-fetched-count (+ fetched-count (+ fetched-count (:limit items)))
         total (:total items)]
     (if (<= total new-fetched-count)
       (:items items)
       (recur new-fetched-count)))))

(defn refresh-items [items]
  (doseq [i items]
    (process-item i)))

(defn update-all-items []
  "Fetch all items from the contentful and do relevant processing and caching."
  (try (-> (get-all-items)
           (refresh-items))
       (catch Exception e
         (log/info "Updating items failed: " e))))

(defn localize-fields [fields locale]
  "Given the :fields part of a multi-locale contentful object, returns
  a localized version. If the localization for the field is not found,
  the default locale one is returned."
  (into {} (for [[k v] fields] [k (get v locale (get v default-locale))])))

(defstate contentful-init :start (update-all-items))
