(ns hiiop.components.cookies-banner
  (:require [rum.core :as rum]
            #?(:cljs [hiiop.context    :refer [context]])
            #?(:cljs [clojure.browser.dom :as dom])
            #?(:cljs [goog.net.cookies :as gcookies])))

(def max-age 2592000)

(rum/defc cookies-banner < rum/reactive []
    #?(:cljs
      (if-let [show-cookies-banner (not (.get goog.net.cookies "hiiop-cookie"))]
          [:div {:class "cookie-consent-bar"} 
            [:button {
                   :on-click (fn []
                               (.set goog.net.cookies
                                     "hiiop-cookie"
                                     true
                                     max-age)
                                (dom/remove-children "cookies-banner"))}
             "Cookies"]])))
