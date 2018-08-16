(ns hiiop.components.cookies-banner
  (:require [rum.core :as rum]
            #?(:cljs [hiiop.context    :refer [context set-context!]])
            #?(:cljs [goog.net.cookies :as gcookies])))

(rum/defc cookies-banner[]
  [:div {:class "cookie-banner-container"}
   #?(:cljs
      (let [show-cookies-banner (:show-cookies-banner context)]
        [:div {:class "cookie-banner"
               :on-click (fn []
                           (.set goog.net.cookies
                                 "show-cookies-banner"
                                 (not show-cookies-banner))
                           (set-context! (assoc context
                                                :show-cookies-banner
                                                (not show-cookies-banner))))}
         "Here is your cookiezzz!"]))])
