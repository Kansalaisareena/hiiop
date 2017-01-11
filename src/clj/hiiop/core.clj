(ns hiiop.core
  (:require [hiiop.handler :as handler]
            [luminus.repl-server :as repl]
            [luminus.http-server :as http]
            [luminus-migrations.core :as migrations]
            [hiiop.config :refer [env]]
            [hiiop.redis :refer [clear-from-cache]]
            [cider.nrepl :refer [cider-nrepl-handler]]
            [clojure.tools.cli :refer [parse-opts]]
            [taoensso.timbre :as log]
            [mount.core :as mount])
  (:gen-class))

(def cli-options
  [["-p" "--port PORT" "Port number"
    :parse-fn #(Integer/parseInt %)]])

(mount/defstate ^{:on-reload :noop}
  http-server
  :start
  (http/start
   (-> env
       (assoc :handler (handler/app))
       (update :port #(or (-> env :options :port) %))))
  :stop
  (http/stop http-server))

(mount/defstate ^{:on-reload :noop}
  repl-server
  :start
  (when-let [nrepl-port (env :nrepl-port)]
    (repl/start {:port nrepl-port :handler cider-nrepl-handler}))
  :stop
  (when repl-server
    (repl/stop repl-server)))


(defn stop-app []
  (doseq [component (:stopped (mount/stop))]
    (log/info component "stopped"))
  (shutdown-agents))

(defn start-app [args]
  (doseq [component (-> args
                        (parse-opts cli-options)
                        mount/start-with-args
                        :started)]
    (log/info component "started"))
  (clear-from-cache :all-moderated-quests)
  (.addShutdownHook (Runtime/getRuntime) (Thread. stop-app)))

(defn -main [& args]
  (cond
    (some #{"migrate" "rollback"} args)
    (do
      (mount/start #'hiiop.config/env)
      (migrations/migrate args (select-keys env [:database-url]))
      (System/exit 0))
    :else
    (start-app args)))
