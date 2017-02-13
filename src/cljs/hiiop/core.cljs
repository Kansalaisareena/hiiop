(ns hiiop.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async             :refer [<!]]
            [rum.core                    :as rum]
            [taoensso.timbre             :as log]
            [bidi.bidi                   :refer [match-route]]
            [mount.core                  :refer [swap]]
            [hiiop.time                  :as time]
            [hiiop.html                  :as html]
            [hiiop.client-config         :refer [env]]
            [hiiop.translate             :refer [tr-with tr-opts]]
            [hiiop.context               :refer [set-context! context]]
            [hiiop.routes.page-hierarchy :as page-routes]
            [hiiop.components.navigation :as navigation]
            [hiiop.client-pages          :as client-pages]))

(enable-console-print!)

(defn mount-top-navigation [context]
  (rum/mount
   (navigation/top-navigation context)
   (. js/document (getElementById "top-navigation"))))

(defn get-config-and-call [this]
  (go
    (let [conf (<! @env)
          client-time (time/now-utc)
          server-time (time/from-string (:now conf))
          time-zone (:time-zone conf)
          hiiop-blog-base-url (:hiiop-blog-base-url conf)
          locale (:current-locale conf)
          diff (time/diff-in-ms server-time client-time)]

      ;; Hide console logs in production
      (when (not (:dev conf))
        (log/set-config! {:enabled? false}))

      (log/info conf)
      (log/info locale diff client-time (.utc (time/from-string server-time)))
      (time/switch-time-zone time-zone)
      (time/switch-locale (keyword locale))
      (time/set-server-client-diff-seconds diff)
      (this conf))))

(defn route! [{:keys [accept-langs langs identity hiiop-blog-base-url] :as conf}]
  (let [tr (tr-with (tr-opts langs) accept-langs)
        routes page-routes/hierarchy
        handler-route-key (match-route routes (.-pathname js/location))
        handler-key (:handler handler-route-key)
        context {:tr tr
                 :conf conf
                 :hierarchy hiiop.routes.page-hierarchy/hierarchy
                 :site-base-url (:site-base-url conf)
                 :hiiop-blog-base-url (:hiiop-blog-base-url conf)
                 :current-locale (keyword (:current-locale conf))
                 :identity (:identity conf)
                 :path-key handler-key}]
    (set-context! context)
    (mount-top-navigation context)
    (log/info handler-route-key handler-key)
    (when (and
           handler-route-key
           (handler-key client-pages/handlers))
      ((handler-key client-pages/handlers) handler-route-key))))

(defn mount-components []
  (get-config-and-call route!))

(defn init! []
  (log/info "INIT")
  (get-config-and-call route!))
