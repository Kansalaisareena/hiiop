(ns hiiop.emails
  (:require [taoensso.timbre :as log]
            [rum.core :as rum]
            [hiiop.time :as time]
            [hiiop.mangling :as mangling]
            [hiiop.config :refer [load-env]]
            [rum.server-render :refer [escape-html]]))

(def mail-style "<style>
                 * { font-family: Calibri, sans-serif;
                     color: #323232; font-weight: 200; }
                 h1 { font-size: 2.3rem; }
                 a { color: #ff6a10; text-decoration: none; }
                 .button-2 { margin-left: 10px; }
                 #footer-image-1 { height: 70px; float: left; margin: 5px; }
                 #footer-image-2 { height: 70px; float: right; margin: 5px; }
                 </style>")

(def footer-images
  [:div {:style {:clear "both" }}
    [:img {:id "footer-image-1"
           :src (str (:asset-base-url (load-env)) "/img/logo_with_text.png")}]
    [:img {:id "footer-image-2"
           :src (str (:asset-base-url (load-env)) "/img/suomi100.png")}]])

(defn quest-details-mail [{:keys [tr
                                  title
                                  quest
                                  body-text
                                  button-text
                                  button-url
                                  message
                                  button2-text
                                  button2-url]}]
  (let [start-time (time/from-string (:start-time quest))
        end-time (time/from-string (:end-time quest))]
    [:html
     [:head {:dangerouslySetInnerHTML {:__html mail-style }}]
     [:body
      [:h1 title]
      [:dl
       [:div {:class "body-text" :dangerouslySetInnerHTML {:__html body-text}}]
        (when message
         [:p {:class "message"} message])
       [:dt (tr [:email.quest.time])]
       [:dd {:class "time"}
        (time/duration-to-print-str start-time end-time)]
       [:dt (tr [:email.quest.place])]
       [:dd {:class "place"}
        (mangling/readable-address (:location quest))]]
      (when button-text
        [:div
         [:a {:href button-url
              :class "button-1"
              :id "button-1"} button-text]])
      (when button2-text
        [:div
         [:a {:href button2-url
              :class "button-2"
              :id "button-2"} button2-text]])]]))

(defn plaintext-quest-details-mail [{:keys [tr
                                            title
                                            quest
                                            body-text-plaintext
                                            button-text
                                            button-url
                                            message
                                            button2-text
                                            button2-url]}]
  (let [start-time (time/from-string (:start-time quest))
        end-time (time/from-string (:end-time quest))]
    (str title "\n\n"
         (tr [:email.quest.time]) " " (time/duration-to-print-str start-time end-time)
         " " (mangling/readable-address (:location quest))
         "\n\n" body-text-plaintext
         (when message
           (str "\n\n" message))

         button-text ": " button-url
         (when button2-text
           (str "\n"
                button2-text ": " button2-url))
         )))

(defn simple-mail [{:keys [title
                           body-text
                           button-text
                           button-url
                           message
                           button2-text
                           button2-url]}]
  [:html
   [:head {:dangerouslySetInnerHTML {:__html mail-style }}]
   [:h1 title]
   [:div {:class "body-text" :dangerouslySetInnerHTML {:__html body-text}}]
   (when message
     [:p {:class "message" :dangerouslySetInnerHTML {:__html message}}])

   [:div [:a {:href button-url
              :class "button-1"
              :id "button-1"} button-text]]
   (when button2-text
     [:div
      [:a {:href button-url
           :class "button-2"
           :id "button-2"} button2-text]])])

(defn plaintext-simple-mail [{:keys [title
                                     body-text-plaintext
                                     button-text
                                     button-url
                                     message
                                     button2-text
                                     button2-url]}]
  (str title "\n\n" body-text-plaintext
       (when message
         (str "\n" message))
       "\n\n"
       button-text ": " button-url
       (when button2-text
         (str "\n"
              button2-text ": " button2-url))))
