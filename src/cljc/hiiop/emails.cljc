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
        end-time (time/from-string (:end-time quest))
        to-p (fn [part] [:p part])
        description-split (filter not-empty (clojure.string/split-lines (:description quest)))
        description (-> (:description quest)
                        (mangling/split-and-trim-lines)
                        (#(map to-p %1)))
        body (-> body-text
                 (mangling/split-and-trim-lines)
                 (#(map to-p %1)))]
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
      (into [:div {:class "description"}] description)
      (into [:div {:class "body-text"}] body)
      (when message
        [:p {:class "message"} message])
      (when button-text
        [:a {:href button-url
             :class "button-1"
             :id "button-1"} button-text])
      (when button2-text
        [:a {:href button2-url
             :class "button-2"
             :id "button-2"} button2-text])]])
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
