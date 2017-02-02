(ns hiiop.components.social-buttons
  (:require [rum.core :as rum]
            [cemerick.url :refer [url-encode]]))

(rum/defc social-buttons [{:keys [title url tr]}]
  [:div {:class "opux-section opux-social-sharing-buttons"}
   (str (tr [:actions.share]) ": ")
   [:a {:class "opux-icon-social opux-icon-social--twitter-share"
        :target "_blanck"
        :href (str "https://twitter.com/intent/tweet?text="
                   (url-encode title)
                   "&via=hiiop100&url="
                   (url-encode url))
        :title "twitter"}]

   [:a {:class "opux-icon-social opux-icon-social--fb-share"
        :target "_blanck"
        :href (str "https://www.facebook.com/sharer/sharer.php?u="
                   (url-encode url)
                   "&t=" (url-encode title))}]])
