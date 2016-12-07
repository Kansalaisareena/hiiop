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
            [hiiop.context               :refer [set-context!]]
            [hiiop.routes.page-hierarchy :as page-routes]
            [hiiop.client-pages          :as client-pages]))

(enable-console-print!)

(defn get-config-and-call [this]
  (go
    (let [conf (<! @env)
          client-time (time/now-utc)
          server-time (time/from-string (:now conf))
          time-zone (:time-zone conf)
          locale (:current-locale conf)
          diff (time/diff-in-ms server-time client-time)]
      (log/info conf)
      (log/info locale diff client-time (.utc (time/from-string server-time)))
      (time/switch-time-zone time-zone)
      (time/switch-locale (keyword locale))
      (time/set-server-client-diff-seconds diff)
      (this conf))))

(defn route! [{:keys [accept-langs langs] :as conf}]
  (let [tr (tr-with (tr-opts langs) accept-langs)
        routes page-routes/hierarchy
        handler-route-key (match-route routes (.-pathname js/location))
        handler-key (:handler handler-route-key)]
    (set-context! {:tr tr :conf conf})
    (log/info handler-route-key handler-key)
    (when (and
           handler-route-key
           (handler-key client-pages/handlers))
      ((handler-key client-pages/handlers) client-pages/handlers))))

(defn mount-components []
  (get-config-and-call route!))

(defn init! []
  (log/info "INIT")
  (get-config-and-call route!))
