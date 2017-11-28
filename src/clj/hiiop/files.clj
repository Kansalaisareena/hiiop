(ns hiiop.files)

(defn create-temp-file []
  (java.io.File/createTempFile "pre" ".suff"))

(defn with-temp-file [f]
  (let [temp-file (create-temp-file)]
    (try
      (f temp-file)
      (catch Exception e
        (throw e))
      (finally (.delete temp-file)))))
