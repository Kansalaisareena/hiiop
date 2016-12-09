(ns hiiop.translate
  (:require [taoensso.tempura :as tempura :refer [tr]]
            [taoensso.encore  :as enc]
            [clojure.set      :refer [intersection]]
            #?(:cljs
               [taoensso.timbre :as log
                :refer-macros [trace  debug  info  warn  error  fatal  report]]
               :clj
               [taoensso.timbre :as log
                :refer [trace debug info warn error fatal report]])))

(def default-locale :fi)

(defn load-resource [filename]
  #?(:clj
     (try
       (info "Loading translation" filename)
       (enc/read-edn (enc/slurp-file-resource filename))
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
  (if (not (empty? accept-langs))
    (let [keyword-accepted-langs (into [] (map keyword accept-langs))
          accept-langs-set (into #{} keyword-accepted-langs)
          langs-set (into #{} (keys langs))
          lang-intersection (intersection langs-set accept-langs-set)
          lang-match (reduce
                      (fn [chosen current]
                        (cond
                          (and chosen (chosen lang-intersection)) chosen
                          (and current (current lang-intersection)) current
                          :else nil)
                        )
                      keyword-accepted-langs)]
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
