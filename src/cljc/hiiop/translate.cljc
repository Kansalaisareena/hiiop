(ns hiiop.translate
  (:require [taoensso.tempura :as tempura :refer [tr]]))

(def langs
  {:fi {:__load-resource "lang/fi.edn"}
   :sv {:__load_resource "lang/sv.edn"}})

(def tr-opts
  {:dict langs :default-locale :fi})

(defn tr-with [lang]
  (partial tr tr-opts [lang]))
