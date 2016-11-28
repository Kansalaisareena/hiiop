(ns hiiop.test.test)

(defn contains-many? [m & ks]
  (every? #(contains? m %) ks))
