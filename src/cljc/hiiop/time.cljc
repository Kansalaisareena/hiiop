(ns hiiop.time
  (:require
   [clojure.string :as string]
   [taoensso.timbre :as log]
   [mount.core :refer [defstate swap]]
   #?(:cljs [goog.string :as gstring])
   #?(:cljs [goog.string.format])
   #?(:clj  [clj-time.core :as time]
      :cljs [cljsjs.moment])
   #?(:cljs [cljsjs.moment.locale.fi])
   #?(:cljs [cljsjs.moment.locale.sv])
   #?(:cljs [cljsjs.moment-timezone])
   #?(:clj [clj-time.coerce :as timec])
   #?(:clj [clj-time.format :as timef])
   [hiiop.mangling :as mangle])
  #?(:clj (:import [org.joda.time MutableDateTime DateTime Days]
                   [java.util Locale])))

#?(:cljs
   (def server-client-diff-seconds (atom 0)))

#?(:cljs
   (defn set-server-client-diff-seconds [diff]
     (reset! server-client-diff-seconds diff)))

(def date-print-format
  #?(:clj "dd.MM.YYYY"
     :cljs "DD.MM.YYYY"))
(def time-print-format "HH.mm")
(def print-format (str date-print-format " " time-print-format))
(def transit-format
  #?(:clj "YYYY-MM-DD'T'HH:mm:ssZ"
     :cljs "YYYY-MM-DDTHH:mm:ssZ"))

(def with-weekday-format
  #?(:clj "EE dd.M"
     :cljs "dd DD.M"))

(def hour-minute-format
  #?(:clj "HH.mm"
     :cljs "HH.mm"))

(def month-name-format
  #?(:clj "MMMM"
     :cljs "MMMM"))

#?(:cljs
   (defstate locale :start :fi))
#?(:cljs
   (defn switch-locale [locale]
     (swap {#'hiiop.time/locale locale})
     (.locale js/moment (name locale))))

(def time-zone (atom "Europe/Helsinki"))
(defn switch-time-zone [time-zone-param]
  (reset! time-zone time-zone-param)
  #?(:clj (time/default-time-zone @time-zone)))


#?(:clj
   (def print-formatter
     (clj-time.format/formatter print-format)))

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

(def a-milli (millis 1))

(defn seconds [amount]
  (if amount
    #?(:clj (time/seconds amount)
       :cljs (.duration js/moment amount "second"))))

(def a-second (seconds 1))

(defn minutes [amount]
  (if amount
    #?(:clj (time/minutes amount)
       :cljs (.duration js/moment amount "minute"))))

(defn get-month [date]
    #?(:clj (time/month date)
       :cljs (.month date)))

(def a-minute (minutes 1))

(defn hours [amount]
  (if amount
    #?(:clj (time/hours amount)
       :cljs (.duration js/moment amount "hour"))))

(def an-hour (hours 1))

(defn days [amount]
  (if amount
    #?(:clj (time/days amount)
       :cljs (.duration js/moment amount "day"))))

(def a-day (days 1))

(defn months [amount]
  (if amount
    #?(:clj (time/months amount)
       :cljs (.duration js/moment amount "month"))))

(def a-month (months 1))

(defn years [amount]
  (if amount
    #?(:clj (time/years amount)
       :cljs (.duration js/moment amount "year"))))

(def a-year (years 1))

(defn year
  ([from]
   #?(:clj (time/year from)
      :cljs (.year from)))
  ([date to]
   #?(:clj
      (let [^MutableDateTime mdt (.toMutableDateTime ^DateTime date)]
        (.toDateTime (doto mdt
                       (.setYear to))))
      :cljs
      (-> (js/moment date)
          (.year to)))))

(defn month
  ([from]
   #?(:clj (time/month from)
      :cljs (.month from)))
  ([date to]
   #?(:clj
      (let [^MutableDateTime mdt (.toMutableDateTime ^DateTime date)]
        (.toDateTime (doto mdt
                       (.setMonthOfYear to))))
      :cljs
      (-> (js/moment date)
          (.month to)))))

(defn day
  ([from]
   #?(:clj (time/day from)
      :cljs (.date from)))
  ([date to]
   #?(:clj
      (let [^MutableDateTime mdt (.toMutableDateTime ^DateTime date)]
        (.toDateTime (doto mdt
                       (.setDayOfMonth to))))
      :cljs
      (-> (js/moment date)
          (.date to)))))

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

(defn time-to
  ([time hours minutes seconds millis]
   #?(:clj
      (let [^MutableDateTime mdt (.toMutableDateTime ^DateTime time)]
        (.toDateTime (doto mdt
                       (.setHourOfDay      hours)
                       (.setMinuteOfHour   minutes)
                       (.setSecondOfMinute seconds)
                       (.setMillisOfSecond millis))))
      :cljs
       (-> (js/moment time)
           (.hours hours)
           (.minutes minutes)
           (.seconds seconds)
           (.milliseconds millis))))
  ([time hours minutes seconds]
   (time-to time hours minutes seconds 0))
  ([time hours minutes]
   (time-to time hours minutes 0)))

(defn use-same-date [from to]
  (let [from-day (day from)
        from-month (month from)
        from-year (year from)]
    (-> (day to from-day)
        (month from-month)
        (year from-year))))

(defn tomorrow []
  (add (now) a-day))

(defn tomorrow-at-noon []
  (time-to (tomorrow) 12 00))

(defn today []
  (time-to (now) 00 00))

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

(defn before? [a b]
  (if (and a b)
    #?(:clj (time/before? a b)
       :cljs (.isBefore a b))))

(defn after? [a b]
  (if (and a b)
    #?(:clj (time/after? a b)
       :cljs (.isAfter a b))))

(defn days-between [a b]
  (if (and a b)
    #?(:clj (.getDays (Days/daysBetween (.toLocalDate a) (.toLocalDate b)))
       :cljs (.diff a b "days"))))

(defn from-string
  ([string]
   #?(:clj  (with-default-time-zone (timec/from-string string))
      :cljs (with-default-time-zone (js/moment string))))
  ([string format]
   #?(:clj  (with-default-time-zone (timef/parse (timef/formatter format) string))
      :cljs (with-default-time-zone (js/moment string format)))))

(defn to-string
  ([date format]
   (if (nil? date)
     ""
     #?(:clj
        (let [formatter (-> (if format
                              (timef/formatter format)
                              (timef/formatters :date-time-no-ms))
                            (timef/with-locale (Locale. "fi")))]
          (-> (timef/with-zone
                formatter
                (time/time-zone-for-id @time-zone))
              (timef/unparse date)))
        :cljs (.format (.tz date @time-zone) format))))
  ([date]
   (if (nil? date)
     ""
     #?(:clj (to-string date nil)
        :cljs (to-string date transit-format)))))

(defn datetime->time [datetime]
  {:hours #?(:clj (time/hour datetime)
             :cljs (.hour datetime))
   :minutes #?(:clj (time/minute datetime)
               :cljs (.minute datetime))})

(defn string->time [time-string]
  (let [numbers (map
                 mangle/parse-natural-number
                 (string/split time-string #"\."))]
    {:hours (first numbers) :minutes (second numbers) }))

(defn time->string [{:keys [hours minutes]}]
  #?(:clj  (format "%02d.%02d" hours minutes)
     :cljs (gstring/format "%02d.%02d" hours minutes)))

(defn time? [time]
  #?(:clj  (instance? org.joda.time.DateTime time)
     :cljs (.isMoment js/moment time)))

(defn duration-to-print-str [start-time end-time]
  (str (to-string
        start-time
        print-format) " - "
       (if (> (days-between
               start-time
               end-time) 0)
         (to-string
          end-time
          print-format)
         (to-string
          end-time
          time-print-format))))
