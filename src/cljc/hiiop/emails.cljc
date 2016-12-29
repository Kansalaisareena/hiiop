(ns hiiop.emails
  (:require [rum.core :as rum]
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
     [:h1 title]
     [:dl
      [:dt (tr [:email.quest.time])]
      [:dd {:class "time"}
       (str (time/to-string
             start-time
             time/print-format) " - "
            (if (> (time/days-between
                    start-time
                    end-time) 0)
              (time/to-string
             (time/from-string (:end-time quest))
             time/print-format)
              (time/to-string
               (time/from-string (:end-time quest))
               time/time-print-format)))]
      [:dt (tr [:email.quest.place])]
      [:dd {:class "place"}
       (mangling/readable-address (:location quest))]]
     [:p body-text]
     (when message
       [:p {:class "message"} message])
     [:a {:href button-url
          :class "button-1"
          :id "button-1"} button-text]
   (when button2-text
     [:a {:href button-url
          :class "button-2"
          :id "button-2"} button2-text])])
  )

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
     [:p {:class "message"} message])
   [:a {:href button-url
        :class "button-1"
        :id "button-1"} button-text]
   (when button2-text
     [:a {:href button-url
          :class "button-2"
          :id "button-2"} button2-text])])
