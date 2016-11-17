(ns hiiop.translate
  (:require [taoensso.tempura :as tempura :refer [tr]]
            [taoensso.encore  :as enc]
            [taoensso.timbre  :as log]))

(defn load-resource [filename]
  #?(:clj
     (try
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
  ([] {:dict langs :default-locale :fi})
  ([param-langs] {:dict param-langs :default-locale :fi}))

(defn tr-with
  ([langs] (partial tr tr-opts langs))
  ([opts langs] (partial tr opts langs)))
