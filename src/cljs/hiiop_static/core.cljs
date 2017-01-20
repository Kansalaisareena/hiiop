(ns hiiop-static.core
  (:require [clojure.string :as str]))

(def toggler
  )

(defn toggleClass [e class]
  (let [current-class (.-className e)]
    (if (str/includes?
          current-class class)
      (set! (.-className e)
            (str/replace
              current-class
              (str " " class)
              ""))
      (set! (.-className e)
            (str current-class " " class)))))

(defn click-handler [e]
  (let [button (.querySelector js/document
                               ".opux-mobile-hamburger__button")
        menu (.querySelector js/document
                             ".opux-nav--header")]
    (.preventDefault e)
    (toggleClass button "is-active")
    (toggleClass menu "is-active")))

(defn init-mobile-menu []
  (let [toggler
        (.querySelector
          js/document
          ".opux-mobile-hamburger__button")]
    (.addEventListener toggler
                       "click"
                       click-handler)))

(init-mobile-menu)
