(ns hiiop-static.core
  (:require [clojure.string :as str]))

(defn toggle-class [e class]
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

;; -----------
;; Mobile Menu
;; -----------
(defn click-handler [e]
  (let [button (.querySelector js/document
                               ".opux-mobile-hamburger__button")
        menu (.querySelector js/document
                             ".opux-nav--header")]
    (.preventDefault e)
    (toggle-class button "is-active")
    (toggle-class menu "is-active")))

(defn init-mobile-menu []
  (let [toggler
        (.querySelector
          js/document
          ".opux-mobile-hamburger__button")]
    (.addEventListener toggler
                       "click"
                       click-handler)))

(init-mobile-menu)

;; -----------
;; Stories filter
;; -----------
(def filtered-categories (atom []))

(defn- set-filtered-categories! []
  (let [story-filters (.querySelectorAll
                       js/document
                       ".opux-category-filter")
        active-story-filters (.querySelectorAll
                              js/document
                              ".opux-category-filter.is-active")
        usable-filters (if (< (.-length active-story-filters) 1)
                         story-filters
                         active-story-filters)
        active-filters (atom [])]
    (.forEach
     usable-filters
     (fn [filter-element]
       (swap! active-filters
              conj (-> filter-element
                       (.getAttribute "filter-data")
                       (js/decodeURIComponent)))))
    (reset! filtered-categories active-filters)))

(defn- filter-click-handler [e]
  (let [story-cards (.querySelectorAll
                     js/document
                     "#opux-stories-list .opux-card-container")
        filter-element (.-target e)
        category (-> (.getAttribute filter-element "filter-data")
                     (js/decodeURIComponent))
        is-active (-> (.-classList filter-element)
                      (.contains "is-active"))]
    (.preventDefault e)
    (toggle-class filter-element "is-active")
    (set-filtered-categories!)))


(defn- add-story-filter-listeners [story-filters]
  (.forEach
   story-filters
   (fn [filter-element]
     (.addEventListener filter-element
                        "click"
                        filter-click-handler))))

(defn- update-story-visibility [story-card]
  (let [card-classes (.-classList story-card)
        is-hidden (.contains card-classes "is-hidden")
        filtered-list (.-tail (.-state @filtered-categories))
        card-categories (-> story-card
                            (.getAttribute "categories")
                            (js/decodeURIComponent)
                            (.split ","))
        should-be-active (> (.-length
                             (.filter filtered-list
                                      #(> (.indexOf card-categories %) -1)))
                            0)]
    (when
        (or
         (and should-be-active is-hidden)
         (and (not should-be-active) (not is-hidden)))
      (toggle-class story-card "is-hidden"))))

(defn- update-visible-stories [filters]
  (let [story-cards (.querySelectorAll
                     js/document
                     ".opux-card-container")]
    (.forEach story-cards update-story-visibility)))

(defn init-stories-filters []
  (let [story-filters (.querySelectorAll
                       js/document
                       ".opux-category-filter")]
    (set-filtered-categories!)
    (add-story-filter-listeners story-filters)

    (add-watch
     filtered-categories :filtered-categories
     (fn [_ _ old new-filters]
       (update-visible-stories new-filters)))))


(init-stories-filters)
