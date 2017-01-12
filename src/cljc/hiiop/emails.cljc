(ns hiiop.emails
  (:require [taoensso.timbre :as log]
            [rum.core :as rum]
            [hiiop.time :as time]
            [hiiop.mangling :as mangling]))

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
     [:body
      [:h1 title]
      [:dl
       [:dt (tr [:email.quest.time])]
       [:dd {:class "time"}
        (time/duration-to-print-str start-time end-time)]
       [:dt (tr [:email.quest.place])]
       [:dd {:class "place"}
        (mangling/readable-address (:location quest))]]
      [:div {:class "body-text" :dangerouslySetInnerHTML {:__html body-text}}]
      (when message
        [:p {:class "message"} message])
      (when button-text
        [:a {:href button-url
             :class "button-1"
             :id "button-1"} button-text])
      (when button2-text
        [:a {:href button2-url
             :class "button-2"
             :id "button-2"} button2-text])]]))

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
   [:h1 title]
   [:p body-text]
   (when message
     [:p {:class "message" :dangerouslySetInnerHTML {:__html message}}])
   [:a {:href button-url
        :class "button-1"
        :id "button-1"} button-text]
   (when button2-text
     [:a {:href button-url
          :class "button-2"
          :id "button-2"} button2-text])])

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
