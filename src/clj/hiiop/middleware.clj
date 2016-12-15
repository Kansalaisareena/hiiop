(ns hiiop.middleware
  (:require [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
            [ring.middleware.webjars :refer [wrap-webjars]]
            [ring.middleware.format :refer [wrap-restful-format]]
            [ring.middleware.flash :refer [wrap-flash]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.util.response :as response]
            [buddy.auth.accessrules :refer [restrict]]
            [buddy.auth.middleware :refer [wrap-authentication]]
            [buddy.auth :refer [authenticated?]]
            [buddy.auth.backends.session :refer [session-backend]]
            [taoensso.timbre :as log :refer [info]]
            [taoensso.tempura :as tempura]
            [taoensso.carmine.ring :refer [carmine-store]]
            [bidi.bidi :as bidi]
            [hiiop.routes.page-hierarchy :refer [hierarchy]]
            [hiiop.env :refer [defaults]]
            [hiiop.config :refer [env]]
            [hiiop.layout :refer [*app-context* error-page]]
            [hiiop.translate :refer [supported-lang tr-opts tr-with]])
  (:import [javax.servlet ServletContext]))

(defn wrap-context [handler]
  (fn [request]
    (binding [*app-context*
              (if-let [context (:servlet-context request)]
                ;; If we're not inside a servlet environment
                ;; (for example when using mock requests), then
                ;; .getContextPath might not exist
                (try (.getContextPath ^ServletContext context)
                     (catch IllegalArgumentException _ context))
                ;; if the context is not specified in the request
                ;; we check if one has been specified in the environment
                ;; instead
                (:app-context env))]
      (handler request))))

(defn wrap-internal-error [handler]
  (fn [req]
    (try
      (handler req)
      (catch Throwable t
        (log/error t)
        (error-page {:status 500
                     :title "Something very bad has happened!"
                     :message "We've dispatched a team of highly trained gnomes to take care of the problem."})))))


(defn auth-error [request response]
  (let [going-to (:handler (bidi/match-route hierarchy (:path-info request)))]
    (response/redirect
     (str
      (bidi/path-for hierarchy :login)
      (when going-to (str "?sitten=" (name going-to)))))))

(defn authenticated [handler]
  (restrict handler {:handler authenticated?
                     :on-error auth-error}))

(defn api-authenticated [handler]
  (restrict handler {:handler authenticated?
                     :on-error (fn [request resp]
                                 (response/status resp 401))}))

(defn wrap-csrf [handler]
  (wrap-anti-forgery
   handler
   {:error-response
    (error-page
     {:status 403
      :title "Invalid anti-forgery token"})}))

(defn wrap-formats [handler]
  (let [wrapped (wrap-restful-format
                 handler
                 {:formats [:json-kw :transit-json :transit-msgpack]})]
    (fn [request]
      ;; disable wrap-formats for websockets
      ;; since they're not compatible with this middleware
      ((if (:websocket? request) handler wrapped) request))))

(def auth-backend (session-backend))

(defn query-string-lang [query-string]
  (when query-string
    (last (re-find #"lang=([^&]+)" query-string))))

(defn cookie-lang [cookies]
  (get-in cookies ["lang" :value]))

(defn override-lang [handler]
  (fn [request]
    (let [query-string (:query-string request)
          lang-qr (query-string-lang query-string)
          lang-cookie (cookie-lang (:cookies request))
          lang (or lang-qr lang-cookie)
          both-set (and lang-qr lang-cookie)
          change-lang (or
                       (and both-set (not (= lang-qr lang-cookie)))
                       (and (not both-set) lang-qr))
          tempura-accepted (if (:tempura/accept-langs request)
                             (:tempura/accept-langs request)
                             [])
          accepted (if lang (into [lang] tempura-accepted) tempura-accepted)
          current-locale (supported-lang accepted)
          tr (tr-with accepted)
          req (if tr
                (assoc request
                       :tempura/tr           tr
                       :tempura/accept-langs accepted
                       :current-locale       (keyword current-locale))
                (assoc request
                       :current-locale (keyword current-locale)))]
      (if change-lang
        (-> (handler req)
            (assoc-in [:cookies "lang" :value]   (name (:current-locale req)))
            (assoc-in [:cookies "lang" :path]    "/")
            (assoc-in [:cookies "lang" :max-age] (* 3600 30)))
        (handler req)))))

(defn wrap-base [handler]
  (-> ((:middleware defaults) handler)
      wrap-webjars
      (wrap-authentication auth-backend)
      override-lang
      (tempura/wrap-ring-request
       {:tr-opts
        (if (:dev env)
          (conj (tr-opts) {:cache-dict? false})
          (tr-opts))})
      (wrap-defaults
       (-> site-defaults
           (assoc-in [:security :anti-forgery] false)
           (assoc-in [:session :store] (carmine-store (env :redis)))))
      wrap-context
      wrap-internal-error))
