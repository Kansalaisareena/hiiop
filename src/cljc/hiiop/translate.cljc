(ns hiiop.translate
  (:require [clojure.set      :refer [intersection]]
            [taoensso.tempura :as tempura :refer [tr]]
            [taoensso.encore  :as enc]
            [taoensso.timbre  :as log]))

(def default-locale :fi)

(defn load-resource [filename & second]
  #?(:clj
     (try
       (log/info "Loading translation" filename)
       (let [content (enc/read-edn (slurp (clojure.java.io/resource filename)))]
         (if (not content)
           (if (not second)
             (load-resource (str "/" filename) :second)
             (throw
              (ex-info "Failed to load dictionary resource"
                       {:filename filename})))
           content))
       (catch Exception e
         (throw
          (ex-info "Failed to load dictionary resource"
                   {:filename filename}))))
     :cljs
     nil))

(def langs
  #?(:clj
     {:fi (load-resource "lang/fi.edn")
      :sv (load-resource "lang/sv.edn")}
     :cljs
     {:fi {:missing "Use /api/v1/config"}
      :sv {:missing "Use /api/v1/config"}}))

(defn tr-opts
  ([] {:dict langs :default-locale default-locale})
  ([param-langs] {:dict param-langs :default-locale default-locale}))

(defn supported-lang [accept-langs]
  (if-not (empty? accept-langs)
    (let [keyword-accepted-langs (into []
                                       (map #(keyword (subs % 0 2))
                                            accept-langs))
          accept-langs-set (into #{} keyword-accepted-langs)
          langs-set (into #{} (keys langs))
          lang-intersection (intersection langs-set accept-langs-set)
          lang-match (first
                       (filter
                         #(contains? lang-intersection %)
                         keyword-accepted-langs))]
      (or lang-match default-locale))
    default-locale))

(defn tr-with
  ([langs]
   (when (first langs)
     (do
       (partial tr (tr-opts) langs))))
  ([opts langs]
   (when (and opts (first langs))
     (do
       (partial tr opts langs)))))
