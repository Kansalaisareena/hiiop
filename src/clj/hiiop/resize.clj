(ns hiiop.resize
  (:require (me.raynes [conch :as sh])
            [clojure.java.io :as io]))

(defn convert-files-on-disk
  "Convert in-file to out-file with specified options"
  [in-file out-file & args]
  (sh/let-programs [convert-command "convert"]
    (apply convert-command (concat args [in-file out-file]))))

(defn resize-to-width
  "Resize to width"
  [file-path width]
  (let [resized-path (str file-path "." (quot (System/currentTimeMillis) 1000))]
    (convert-files-on-disk file-path
                           resized-path
                           "-resize"
                           width)
    (io/file resized-path)))
