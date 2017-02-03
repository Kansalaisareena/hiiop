(ns hiiop.components.pikaday
  (:require [taoensso.timbre :as log]
            [rum.core :as rum]
            [hiiop.time :as time]
            #?(:cljs [cljsjs.pikaday])
            [camel-snake-kebab.core :refer [->camelCaseString]]
            [camel-snake-kebab.extras :refer [transform-keys]]))

(defn- opts-transform [opts]
  "Given a clojure map of options, return a js object."
  #?(:cljs
     (clj->js (transform-keys ->camelCaseString opts))
     :clj opts))

(defn pikaday [args]
  #?(:cljs
     (js/Pikaday. (opts-transform args))
     :clj nil))

(defn pikaday-destroy [instance]
  (when instance
    #?(:cljs (.destroy instance)
       :clj nil)))

(def pikaday-mixin
  {:did-mount
   (fn [state]
     (log/trace state)
     (let [args (first (:rum/args state))
           current-locale (keyword (get-in args [:context :conf :current-locale]))
           translations (get-in args [:context :conf :langs current-locale :pikaday])
           date (:date args)
           format (:format args)
           position (:position args)
           instance (pikaday
                      {:field (.querySelector (rum/dom-node state) "input")
                       :trigger (.querySelector (rum/dom-node state) ".opux-date-picker-trigger")
                      :first-day (:first-day translations)
                      :i18n translations
                      :date (time/to-string @date format)
                      :format format
                      :show-week-number true
                      :position position
                      :reposition false
                      :on-select
                      (fn [chosen-time]
                        (swap! date #(time/from-string chosen-time format)))})]
       (assoc state
              ::instance instance)))

   :will-unmount
   (fn [state]
     (log/trace "destroy" (::instance state))
     (pikaday-destroy (::instance state))
     (dissoc state ::instance))
   })
