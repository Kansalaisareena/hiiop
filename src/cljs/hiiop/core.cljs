(ns hiiop.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async             :refer [<!]]
            [rum.core                    :as rum]
            [taoensso.timbre             :as log]
            [bidi.bidi                   :refer [match-route]]
            [hiiop.html                  :as html]
            [hiiop.client-config         :refer [env]]
            [hiiop.translate             :refer [tr-with tr-opts]]
            [hiiop.context               :refer [set-context!]]
            [hiiop.routes.page-hierarchy :as page-routes]
            [hiiop.client-pages          :as client-pages]))

(enable-console-print!)

(defn get-config-and-call [this]
  (go
    (let [conf (<! @env)]
      (this conf))))

(defn route! [{:keys [accept-langs langs] :as conf}]
  (let [tr (tr-with (tr-opts langs) accept-langs)
        routes page-routes/hierarchy
        handler-route-key (match-route routes (.-pathname js/location))]
    (set-context! {:tr tr :conf conf})
    (when (:handler handler-route-key) (((:handler handler-route-key) client-pages/handlers)))))

(defn mount-components []
  (get-config-and-call route!))

(defn init! []
  (log/info "INIT")
  (get-config-and-call route!))
