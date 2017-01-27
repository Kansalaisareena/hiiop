(ns hiiop.components.autolink
  (:require [rum.core :as rum]))

(def autolink-mixin
  {:did-mount
   (fn [state]
     #?(:cljs
        (let [auto-linker
              (new js/Autolinker
                   {:urls {:schemaMatches true
                           :wwwMatches true
                           :tldMatches true}
                    :email true
                    :mention false
                    :hashtag "twitter"
                    :stripPrefix true
                    :newWindow true
                    :truncate {:length 0
                               :location "end"}
                    :className ""})
              auto-link-targets (.querySelectorAll js/document ".opux-auto-link")]
          (when auto-link-targets
            (.forEach auto-link-targets
                      (fn [target index]
                        (.log js/console "LINK TEXT===="
                              (.link auto-linker
                                     (.-innerHTML target)))
                        (set! (. (aget auto-link-targets index) -innerHTML)
                              (.link auto-linker
                                     (.-innerHTML target)))))))))})
