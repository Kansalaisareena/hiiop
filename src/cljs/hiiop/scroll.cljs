(ns hiiop.scroll)

(defn scroll-to [el]
  (when el
    (-> (.getBoundingClientRect el)
        (.-top)
        (#(.scrollTo js/window 0 %1)))))

(defn scroll-top []
  (.scrollTo js/window 0 0))
