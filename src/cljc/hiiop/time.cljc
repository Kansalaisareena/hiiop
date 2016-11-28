(ns hiiop.time
  (:require
   [taoensso.timbre :as log]
   [mount.core :refer [defstate swap]]
   #?(:clj[clj-time.core :as time]
      :cljs[cljsjs.moment])
   #?(:cljs [cljsjs.moment.locale.fi])
   #?(:cljs [cljsjs.moment.locale.sv])
   #?(:clj [clj-time.coerce :as timec])
   #?(:clj [clj-time.format :as timef])))

#?(:cljs
   (def server-client-diff-seconds (atom 0)))

#?(:cljs
   (defn set-server-client-diff-seconds [diff]
     (swap! server-client-diff-seconds (fn [] diff))))

(def print-format "dd.MM.yyyy hh.mm")

#?(:cljs (defstate locale :start :fi))
#?(:cljs (defn switch-locale [locale]
           (swap {#'hiiop.time/locale locale})
           (.locale js/moment (name locale))))

(def time-zone (atom "Europe/Helsinki"))
(defn switch-time-zone [time-zone-param]
  (swap! time-zone (fn [] time-zone-param))
  #?(:clj (time/default-time-zone @time-zone)))


#?(:clj
   (def print-formatter (clj-time.format/formatter print-format)))

(defn with-default-time-zone [time]
  #?(:clj  (time/to-time-zone time (time/time-zone-for-id @time-zone))
     :cljs (.tz time @time-zone)))

(defn now []
  #?(:clj  (with-default-time-zone (time/now))
     :cljs (with-default-time-zone (.add (.utc (js/moment)) @server-client-diff-seconds "milliseconds"))))

(defn now-utc []
  #?(:clj  (time/to-time-zone (time/now) (clj-time.core/time-zone-for-id "UTC"))
     :cljs (.add (.utc (js/moment)) @server-client-diff-seconds "milliseconds")))

(defn today-midnight []
  #?(:clj (time/today-at-midnight)
     :cljs (.startOf (now) "day")))

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

(defn millis [amount]
  (if amount
    #?(:clj (time/millis amount)
       :cljs (.duration js/moment amount "millisecond"))))

(def milli (millis 1))

(defn seconds [amount]
  (if amount
    #?(:clj (time/seconds amount)
       :cljs (.duration js/moment amount "second"))))

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
  #?(:clj  (with-default-time-zone (timec/from-string string))
     :cljs (with-default-time-zone (js/moment string))))

(defn to-string [date]
  #?(:clj (timef/unparse
           (timef/with-zone
             (timef/formatters :date-time-no-ms)
             (time/time-zone-for-id @time-zone))
           date)
     :cljs (.format (.tz date @time-zone) "YYYY-MM-DDTHH:mm:ssZ")))
