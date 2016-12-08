(ns hiiop.components.login
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require
   [rum.core :as rum]
   [schema.core :as s]
   [taoensso.timbre :as log]
   [bidi.bidi :refer [path-for]]
   #?(:cljs [cljs.core.async :refer [<!]])
   #?(:cljs [hiiop.client-api :as api])
   [hiiop.schema :as schema]
   [hiiop.html :as html]
   [hiiop.url :as u]
   [hiiop.routes.page-hierarchy :as pages]))

(rum/defc login [{:keys [context]}]
  (let [tr (:tr context)
        credentials (atom {:email "" :password ""})
        user (rum/cursor credentials :email)
        password (rum/cursor credentials :password)]
    [:form
     {:on-submit
      (fn [e]
        (.preventDefault e)
        #?(:cljs
           (go
             (let [status (<! (api/login @credentials))
                   sitten (keyword (:sitten (u/query-params (.-href js/location))))
                   to-key (or sitten :index)
                   to (or (path-for pages/hierarchy to-key) (path-for pages/hierarchy :index))]
               (when status
                 (set! (.-pathname js/location) to))))
           ))}
     [:h2 (tr [:pages.login.title])]
     (html/label
      (tr [:pages.login.user])
      {:class "user-label"}
      (html/input
       {:schema s/Str
        :value user
        :type "text"
        :class "email"
        :error (atom nil)}))
     (html/label
      (tr [:pages.login.password])
      {:class "password-label"}
      (html/input
       {:schema s/Str
        :value password
        :type "password"
        :class "password"
        :error (atom nil)}))
     [:input
      {:type "submit"
       :value (tr [:actions.user.login])}]]))
