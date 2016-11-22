(ns hiiop.context)

(def context (atom {}))

(defn set-context! [new-context]
  (swap! context conj new-context))
