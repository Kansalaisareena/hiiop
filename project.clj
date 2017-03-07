(defproject hiiop "0.0.1-SNAPSHOT"

  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :repositories {"project" "file:maven_repository"}

  :dependencies [[bouncer                                "1.0.0"]
                 [cider/cider-nrepl                      "0.14.0"]
                 [cljs-http                              "0.1.42"]
                 [compojure                              "1.5.1"]
                 [conman                                 "0.6.2"]
                 [cheshire                               "5.6.3"]
                 [cprop                                  "0.1.9"]
                 [funcool/cuerdas                        "2.0.1"]
                 [camel-snake-kebab                      "0.4.0"]
                 [luminus-immutant                       "0.2.2"]
                 [luminus-migrations                     "0.2.8"]
                 [luminus-nrepl                          "0.1.4"]
                 [metosin/compojure-api                  "1.1.9"]
                 [metosin/ring-http-response             "0.8.0"]
                 [mount                                  "0.1.10"]
                 [org.clojure/clojure                    "1.8.0"]
                 [org.clojure/clojurescript              "1.9.293" :scope "provided"]
                 [org.clojure/tools.cli                  "0.3.5"]
                 [org.clojure/tools.logging              "0.3.1"]
                 [org.clojure/math.numeric-tower         "0.0.4"]
                 [org.postgresql/postgresql              "9.4.1211"]
                 [org.webjars.bower/tether               "1.3.7"]
                 [org.webjars/bootstrap                  "4.0.0-alpha.5"]
                 [org.webjars/font-awesome               "4.6.3"]
                 [org.webjars/webjars-locator-jboss-vfs  "0.1.0"]
                 [ring-middleware-format                 "0.7.0"]
                 [ring-webjars                           "0.1.1"]
                 [ring/ring-defaults                     "0.2.1"]
                 [rum                                    "0.10.8"]
                 [selmer                                 "1.10.0"]
                 [clj-time                               "0.12.2"]
                 [buddy/buddy-auth                       "1.2.0"]
                 [buddy/buddy-hashers                    "1.0.0"]
                 [com.taoensso/carmine                   "2.15.0"]
                 [com.taoensso/tempura                   "1.0.0-RC4"]
                 [com.taoensso/encore                    "2.87.0"]
                 [com.taoensso/timbre                    "4.7.4"]
                 [com.draines/postal                     "2.0.2"]
                 [bidi                                   "2.0.14"]
                 [metosin/schema-tools                   "0.9.0"]
                 [com.novemberain/pantomime              "2.8.0"]
                 [com.cemerick/url                       "0.1.1"]
                 [amazonica                              "0.3.78"
                  :exclusions [com.amazonaws/aws-java-sdk
                               com.amazonaws/amazon-kinesis-client
                               com.fasterxml.jackson.core/jackson-annotations
                               joda-time
                               org.apache.httpcomponents/httpclient
                               com.fasterxml.jackson.core/jackson-databind
                               ]]
                 [com.amazonaws/aws-java-sdk-core        "1.11.63"
                  :exclusions [commons-logging]]
                 [com.amazonaws/aws-java-sdk-s3          "1.11.63"
                  :exclusions [commons-logging]]
                 [cljsjs/moment                          "2.15.2-3"]
                 [cljsjs/moment-timezone                 "0.5.10-0"]
                 [cljsjs/pikaday                         "1.4.0-1"]
                 [cljsjs/dropzone                        "4.3.0-0"]
                 [clj-http                               "2.3.0"]
                 [com.atlassian.commonmark/commonmark    "0.8.0"]
                 [com.cemerick/url                       "0.1.1"]]

  :min-lein-version "2.5.3"
  :jvm-opts ["-server" "-Dconf=.lein-env" "-Duser.timezone=Europe/Helsinki"]
  :source-paths ["src/clj" "src/cljc"]
  :resource-paths ["resources" "target/cljsbuild"]
  :target-path "target/%s/"
  :main hiiop.core
  :uberjar-name "hiiop.jar"

  :migratus
  {:store :database :db ~(get (System/getenv) "DATABASE_URL")}

  :plugins
  [[lein-resource "16.9.1"]
   [lein-cprop "1.0.1"]
   [migratus-lein "0.4.3"]
   [lein-cljsbuild "1.1.5"]
   [lein-immutant "2.1.0"]
   [lein-sassc "0.10.4"]
   [lein-auto "0.1.2"]
   [lein-asset-minifier "0.3.0"]
   [shmish111/lein-git-version "1.0.13"]
   [lein-essthree "0.2.2"
    :exclusions
    [amazonica
     org.apache.commons/commons-compress
     com.fasterxml.jackson.core/jackson-core]]
   [lein-heroku "0.5.3"
    :exclusions
    [commons-codec
     org.apache.commons/commons-compress
     com.fasterxml.jackson.core/jackson-core]]]

  :sassc
  [{:src "resources/scss/screen.scss"
    :output-to "resources/public/css/screen.css"
    :style "compressed"
    :import-path "resources/scss"}]

  :auto
  {"sassc" {:file-pattern #"\.(scss|sass)$" :paths ["resources/scss"]}}

  :hooks [leiningen.sassc]
  :clean-targets ^{:protect false}
  [:target-path [:cljsbuild :builds :app :compiler :output-dir] [:cljsbuild :builds :app :compiler :output-to]]
  :figwheel
  {:http-server-root "public"
   :nrepl-port 7002
   :server-port 3450
   :css-dirs ["resources/public/css"]
   :nrepl-middleware [cider.nrepl/cider-middleware
                      refactor-nrepl.middleware/wrap-refactor
                      cemerick.piggieback/wrap-cljs-repl]}

  :heroku {:app-name      ~(get (System/getenv) "HEROKU_APP")
           :jdk-version   "1.8"
           :include-files ["target/uberjar/hiiop.jar"]
           :process-types { "web" "java -jar $JVM_OPTS target/uberjar/hiiop.jar" }}

  :resource
  {:resource-paths [["target/cljsbuild/public/js"
                     {:skip-stencil [#"target/cljsbuild/public/js.*"]
                      :target-path
                      ~(str "resources/public/"
                            (apply
                             str
                             (clojure.string/trim
                              (:out
                               (clojure.java.shell/sh
                                "git" "rev-parse" "--verify" "HEAD"))))
                            "/js")}]
                    ["resources/public/css"
                     {:target-path
                      ~(str "resources/public/"
                            (apply
                             str
                             (clojure.string/trim
                              (:out
                               (clojure.java.shell/sh
                                "git" "rev-parse" "--verify" "HEAD"))))
                            "/css")}]]}

  :essthree
  {:deploy {:type       :directory
            :bucket     ~(get (System/getenv) "HIIOP_ASSET_BUCKET")
            :local-root "resources/public/"
            }}

  :profiles
  {:uberjar
   {:aot :all
    :uberjar-name "hiiop.jar"
    :source-paths ["env/prod/clj"]
    :resource-paths ["env/prod/resources"]

    :omit-source true
    ;;:prep-tasks ["git-version" "compile" ["cljsbuild" "once" "min"] "resource" "minify-assets"]
    :prep-tasks ["git-version"
                 "compile"
                 ["cljsbuild" "once" "min"]
                 ["cljsbuild" "once" "static"]
                 "resource"]
    :cljsbuild
    {:builds
     {:min
      {:source-paths ["src/cljc/hiiop"
                      "src/cljs/hiiop"
                      "env/prod/cljs/hiiop"]
       :compiler
       {:output-to "target/cljsbuild/public/js/app.js"
        :externs ["externs/google_maps_api_v3.js"]
        :optimizations :advanced
        :parallel-build true
        :compiler-stats true
        :pretty-print false
        :verbose true
        :source-map false}}

      :static
      {:source-paths ["src/cljs/hiiop_static"]
       :compiler
       {:output-to "target/cljsbuild/public/js/static.js"
        :externs ["externs/google_maps_api_v3.js"]
        :optimizations :advanced
        :parallel-build true
        :verbose true
        :compiler-stats true
        :pretty-print false
        :source-map false}}}}}

   :dev           [:project/dev :profiles/dev]
   :test          [:project/dev :project/test :profiles/test]

   :project/dev  {:dependencies
                  [[prone                        "1.1.2"]
                   [ring/ring-mock               "0.3.0"]
                   [ring/ring-devel              "1.5.0"]
                   [pjstadig/humane-test-output  "0.8.1"]
                   [binaryage/devtools           "0.8.2"]
                   [com.cemerick/piggieback      "0.2.2-SNAPSHOT"]
                   [doo                          "0.1.7"]
                   [figwheel-sidecar             "0.5.8"]
                   [criterium                    "0.4.4"]]
                  :plugins
                  [[com.jakemccrary/lein-test-refresh  "0.18.0"]
                   [lein-doo                           "0.1.7"]
                   [lein-figwheel                      "0.5.8"]
                   [org.clojure/clojurescript          "1.9.293"]
                   [lein-autoreload                    "0.1.1"]]

                  :prep-tasks ["git-version"]

                  :cljsbuild
                  {:builds
                   {:app
                    {:source-paths ["src/cljs/hiiop" "src/cljc" "env/dev/cljs"]
                     :compiler
                     {:main "hiiop.app"
                      :asset-path "/js/out"
                      :output-to "target/cljsbuild/public/js/app.js"
                      :output-dir "target/cljsbuild/public/js/out"
                      :source-map true
                      :optimizations :none
                      :pretty-print true}}
                    :static
                    {:source-paths ["src/cljs/hiiop_static"]
                     :compiler
                     {:main "hiiop.static"
                      :output-to "target/cljsbuild/public/js/static.js"
                      :optimizations :whitespace
                      :pretty-print true}}}}

                  :doo {:build "test"}
                  :source-paths ["env/dev/clj" "test/clj"]
                  :resource-paths ["env/dev/resources"]
                  :repl-options {:init-ns user :timeout 1200000}
                  :injections [(require 'pjstadig.humane-test-output)
                               (pjstadig.humane-test-output/activate!)]}
   :project/test {:resource-paths ["env/test/resources"]
                  :cljsbuild
                  {:builds
                   {:test
                    {:source-paths ["src/cljc" "src/cljs" "test/cljs"]
                     :compiler
                     {:output-to "target/test.js"
                      :main "hiiop.doo-runner"
                      :optimizations :whitespace
                      :pretty-print true}}}}

                  :prep-tasks ["git-version"]

                  }
   :profiles/dev {}
   :profiles/test {}})
