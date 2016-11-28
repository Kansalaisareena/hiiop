(ns hiiop.time
  (:require
   [taoensso.timbre :as log]
   #?(:clj[clj-time.core :as time]
      :cljs[cljsjs.moment])
   #?(:cljs [cljsjs.moment.locale.fi])
   #?(:cljs [cljsjs.moment.locale.sv])
   #?(:clj [clj-time.coerce :as timec])))

#?(:cljs
   (def server-client-diff-seconds (atom 0)))

#?(:cljs
   (defn set-server-client-diff-seconds [diff]
     (swap! server-client-diff-seconds (fn [] diff))))

(def locale (atom "fi"))
(def print-format "dd.MM.yyyy hh.mm")

#?(:clj
   (def print-formatter (clj-time.format/formatter print-format)))

(defn set-locale [locale]
  (swap! locale (fn [] locale))
  #?(:cljs (.locale js/moment @locale)))

(defn now []
  #?(:clj (time/now)
     :cljs (.add (js/moment) @server-client-diff-seconds "milliseconds")))

(defn today-midnight []
  #?(:clj (time/today-at-midnight)
     :cljs (.startOf (js/moment) "day")))

(defn diff-in-ms [a b]
  (if (and a b)
    #?(:clj (- (timec/to-long a) (timec/to-long b))
       :cljs (.diff a b))
    nil))

(defn to-unit [string]
  #?(:clj
     (case string
       "millisecond"  time/millis
       "milliseconds" time/millis
       "second"       time/seconds
       "seconds"      time/seconds
       "minute"       time/minutes
       "minutes"      time/minutes
       "hour"         time/hours
       "hours"        time/hours
       "day"          time/days
       "days"         time/days
       "month"        time/months
       "months"       time/months
       "year"         time/years
       "years"        time/years)
     :cljs string))

(defn to-unit [string]
  #?(:clj
     (case string
       "millisecond"  time/millis
       "milliseconds" time/millis
       "second"       time/seconds
       "seconds"      time/seconds
       "minute"       time/minutes
       "minutes"      time/minutes
       "hour"         time/hours
       "hours"        time/hours
       "day"          time/days
       "days"         time/days
       "month"        time/months
       "months"       time/months
       "year"         time/years
       "years"        time/years)
     :cljs string))

(defn millis [amount]
  (if amount
    #?(:clj (time/millis amount)
       :cljs (.duration js/moment amount "millisecond"))))

(def milli (millis 1))

(defn seconds [amount]
  (if amount
    #?(:clj (time/seconds amount)
       :cljs (.duration js/moment amount "second"))))

(def second (seconds 1))

(defn minutes [amount]
  (if amount
    #?(:clj (time/minutes amount)
       :cljs (.duration js/moment amount "minute"))))

(def minute (minutes 1))

(defn hours [amount]
  (if amount
    #?(:clj (time/hours amount)
       :cljs (.duration js/moment amount "hour"))))

(def hour (hours 1))

(defn days [amount]
  (if amount
    #?(:clj (time/days amount)
       :cljs (.duration js/moment amount "day"))))

(def day (days 1))

(defn months [amount]
  (if amount
    #?(:clj (time/months amount)
       :cljs (.duration js/moment amount "month"))))

(def month (months 1))

(defn years [amount]
  (if amount
    #?(:clj (time/years amount)
       :cljs (.duration js/moment amount "year"))))

(def year (years 1))

(defn add
  ([to amount]
   (if (and to amount)
     #?(:clj  (time/plus to amount)
        :cljs (.add to amount))))
  ([to amount units]
   (if (and to amount units)
     #?(:clj  (time/plus to ((to-unit units) amount))
        :cljs (.add to amount units)))))

(defn subtract
  ([from amount]
   (if (and from amount)
     #?(:clj  (time/minus from amount)
        :cljs (.subtract from amount))))

  ([from amount units]
   (if (and from amount units)
     #?(:clj  (time/minus from ((to-unit units) amount))
        :cljs (.subtract from amount units)))))

(defn relative-string
  "Returns relative time on client side and date on server side"
  ([a]
   (if a
     #?(:clj (clj-time.format/unparse print-formatter a)
        :cljs (.from a (now)))))
  ([a b]
   (if (and a b)
     #?(:clj (clj-time.format/unparse print-formatter a)
        :cljs (.from a b)))))

(defn from-string [string]
  #?(:clj (timec/from-string string)
     :cljs (js/moment string)))
