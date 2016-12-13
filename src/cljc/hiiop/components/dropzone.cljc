(ns hiiop.components.dropzone
  (:require [rum.core :as rum]
            #?(:cljs [cljsjs.dropzone])
            [taoensso.timbre :as log]
            [hiiop.mangling :as m]))

#?(:cljs (set! (.-autoDiscover js/Dropzone) false))

(defn dropzone [dom-arg options]
  #?(:cljs
     (-> options
         (m/->keys-camelCase)
         (clj->js)
         (#(js/Dropzone. dom-arg %1)))
     :clj nil))

(defn on-state-change-set! [tr transform value error event]
  #?(:cljs
     (let [server-response (.. event -target -response)]
       (when (and server-response (> (count server-response) 0))
         (-> (js->clj (.parse js/JSON (.. event -target -response))
                      :keywordize-keys true)
             (#(if (:errors %1)
                 (reset! error
                         (tr))
                 (reset! value (transform (:id %1)))))
             )
         ))))

(def dropzone-mixin
  {:did-mount
   (fn [state]
     (let [args (first (:rum/args state))
           transform (or (:transform args) identity)
           tr (:tr args)
           set-value-or-error! (partial
                                on-state-change-set!
                                tr
                                transform
                                (:value args)
                                (:error args))]
       (assoc
        state
        ::instance
        (-> (dropzone
             (rum/dom-node state)
             {:url "/api/v1/pictures/add"
              :headers {"Cache-Control" ""
                        "X-Requested-With" ""}
              :max-files 1
              :clickable true
              :add-remove-links true
              })
            #?(:cljs
               (.on "sending"
                    (fn [_ xhr]
                      (set! (.-onreadystatechange xhr)
                            set-value-or-error!))))))
        ))})
