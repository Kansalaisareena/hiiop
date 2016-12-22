(ns hiiop.html
  (:require [rum.core :as rum]
            [taoensso.timbre :as log]
            [clojure.string :as string]
            [schema.core :as s]
            #?(:cljs [schema.core :refer [EnumSchema]])
            [schema-tools.core :as st]
            [schema-tools.coerce :as stc]
            [bidi.bidi :refer [path-for]]
            [hiiop.components.dropzone :refer [dropzone-mixin]]
            [hiiop.components.core :as c]
            [hiiop.components.navigation :as navigation]
            [hiiop.components.pikaday :refer [pikaday pikaday-mixin]]
            [hiiop.components.address-autocomplete :as address]
            [hiiop.time :as time]
            [hiiop.schema :as hs]
            [hiiop.mangling :as mangling])
  #?(:clj (:import [schema.core EnumSchema])))

(rum/defc page [head body]
  [:html head body])

(defn class-label-with-error [error class]
  (let [class-str (if class
                    (str class)
                    "")]
    (if (and error (deref error))
      (str class " opux-input__label--error")
      class-str)))

(defn class-error-or-hide [error]
  (if (and error (deref error))
    (str "error " (name (deref error)))
    "hide"))

(defn save-value-or-error [coercer value error new-value]
  (let [value-or-error (hs/either coercer new-value)
        coerced-value (:--value value-or-error)
        coerced-error (:--error value-or-error)]
    (if coerced-value
      (do
        (reset! value coerced-value)
        (reset! error nil))
      (reset! error coerced-error))))

(rum/defcs label < rum/reactive
                   (rum/local nil ::error)
  [state text {:keys [for class error] :as or-content} & content]
  (let [local-error (::error state)
        also-content (if (sequential? or-content)
                       (into [] or-content)
                       [])
        more-content (if content
                       (into [] content)
                       [])
        content-vector (into [] (concat more-content also-content))
        default-content [:label
                         {:class
                          (if (or error (and error (rum/react error)))
                            (class-label-with-error error class)
                            class)
                          :for for}
                         text]]
    (when error
      (add-watch
       error
       ::label-error
       (fn [key _ _ new]
         (reset! local-error new))))
    (into default-content content-vector)))

(defn value-from-event [e & with-transform]
  (let [transform (or (first with-transform) identity)]
    (-> #?(:cljs (.. e -target -value)
           :clj (get-in e [:target :value]))
        (transform))))

(defn checked-from-event [e]
  (-> #?(:cljs (.. e -target -checked)
         :clj (get-in e [:target :checked]))))

(rum/defc button < rum/reactive
  [text {:keys [class active type on-click]}]
  (let [click (or on-click identity)]
    [:button
     {:class class
      :type (or type "button")
      :disabled (when active (not (rum/react active)))
      :on-click
      (fn [e]
        (click e))}
     text]))

(rum/defc input < rum/reactive
  [{:keys [type value schema matcher error class to-value transform-value context error-key]}]
  (let [tr (:tr context)
        usable-matcher (if (not matcher) {schema #(identity %)} matcher)
        usable-transform (if (not transform-value) #(identity %) transform-value)
        usable-to-value (if (not to-value) identity to-value)
        coercer (stc/coercer schema usable-matcher error-key)
        save-val-or-error (partial save-value-or-error coercer value error)]
    [:div
     {:class "opux-input-container"}
     [:input
      {:type type
       :class class
       :value (usable-to-value (rum/react value))
       :on-change
       (fn [e]
         (-> (value-from-event e usable-transform)
             (save-val-or-error)))}]
     (when (rum/react error)
       [:span
        {:class (class-error-or-hide error)}
        (if (rum/react error) (tr [(rum/react error)]))])]))

(rum/defc number-input-with-ticker < rum/reactive
  [{:keys [type value schema error class context min-value max-value error-key]}]
  (let [tr (:tr context)
        coercer (stc/coercer schema {schema #(identity %)} error-key)
        save-val-or-error (partial save-value-or-error coercer value error)
        ]
    [:div {:class "opux-input__container opux-input__container--number-tick"}
     [:span {:class "opux-number-tick-input__control opux-icon opux-icon-minus"
       :on-click (fn [e]
                   (.preventDefault e)
                   (save-val-or-error (- @value 1)))}]
     (input
      {:class (str "opux-input opux-input--number-ticker " class)
       :type "number"
       :error error
       :value value
       :schema schema
       :transform-value #(if (string? %) (mangling/parse-natural-number %))
       :context context})

     [:span {:class "opux-number-tick-input__control opux-icon opux-icon-plus"
      :on-click (fn [e]
                  (.preventDefault e)
                  (save-val-or-error (inc @value)))}]]))

(rum/defc text < rum/reactive
  [{:keys [label value schema matcher error class context error-key]}]
  (let [tr (:tr context)
        usable-matcher (if (not matcher) {schema #(identity %)} matcher)
        coercer (stc/coercer schema usable-matcher error-key)
        save-val-or-error (partial save-value-or-error coercer value error)]
    [:div
     {:class "input-container"}
     [:textarea
      {:class class
       :default-value (rum/react value)
       :on-change
       (fn [e]
         (-> (value-from-event e)
             (save-val-or-error)))}]
     [:span
      {:class (class-error-or-hide error)}
      (if (rum/react error) (tr [(rum/react error)]))]]))

(rum/defc select < rum/reactive
  [{:keys [value options class transform error]}]
  (let [html-options (map
                      #(identity
                        [:option
                         {:value (:value %)}
                         (:text %)])
                      options)
        usable-transform (or transform identity)]
    (into
     []
     (concat
      [:select
       {:class class
        :default-value (rum/react value)
        :on-change
        (fn [e]
          (let [event-value (value-from-event e usable-transform)]
            (reset! value event-value)))}]
      html-options))))

(rum/defcs datepicker < pikaday-mixin
  [state {:keys [date min-date max-date format schema error class transform-value context error-key]}]
  [:div {:class
         (str "opux-input__container opux-input__container--date-picker opux-icon "
              class)}
   [:input
    {:type "text"
     :class "opux-input opux-input--date-picker"
     :default-value (time/to-string @date format)}]])

(rum/defc timepicker < rum/reactive
  [{:keys [time class time-print-format context]}]
  (let [hour-steps (into [] (range 00 24))
        minute-steps (into [] (range 00 60 15))
        steps-list (flatten
                    (map
                     (fn [hour]
                       (map
                        (fn [minute]
                          (time/time->string {:hours hour :minutes minute}))
                        minute-steps))
                     hour-steps))
        steps (into [] steps-list)
        options (into [] (map #(identity {:value % :text %}) steps))
        select-value-atom (atom (time/time->string @time))]
    (add-watch
     select-value-atom
     :timepicker-select
     (fn [key _ _ new-time]
       (reset! time new-time)))
    (select
     {:options options
      :class class
      :value select-value-atom
      :transform time/string->time})))

(defn set-error! [error to]
  (reset! error to))

(rum/defc datetime-picker < rum/reactive
  [{:keys [date min-date max-date error schema class date-print-format time-print-format value-format context]}]
  (let [tr (:tr context)
        set-datetime-error! (partial set-error! error)
        min-date-object (fn []
                          (when min-date
                            (time/from-string @min-date)))
        max-date-object (fn []
                          (when max-date
                            (time/from-string @max-date)))
        is-after-min? (fn [new-date]
                        (or (not min-date)
                            (time/before? (min-date-object) new-date)))
        is-before-max? (fn [new-date]
                         (or (not max-date)
                             (time/after? (max-date-object) new-date)))
        date-object (time/from-string @date value-format)
        date-atom (atom date-object)
        time-atom (atom (time/datetime->time date-object))
        print-date-time-format (str date-print-format " " time-print-format)
        set-date! (fn [new-datetime]
                    (reset! date
                            (time/to-string new-datetime value-format)))
        is-invalid? (fn [new-datetime]
                      (cond
                        (not (is-after-min? new-datetime))
                        (tr [:error.date-needs-to-be-before]
                            [(time/to-string (min-date-object) print-date-time-format)])
                        (not (is-before-max? new-datetime))
                        (tr [:error.date-needs-to-be-after]
                            [(time/to-string (max-date-object) print-date-time-format)])
                        :else nil))]
    (add-watch
     date-atom
     :date
     (fn [key _ old-date new-date]
       (log/trace key @date-atom @time-atom)
       (let [new-datetime (time/time-to
                           new-date
                           (:hours @time-atom)
                           (:minutes @time-atom))
             error-message (is-invalid? new-datetime)]
         (if (not error-message)
           (do
             (set-date! new-datetime)
             (set-datetime-error! nil))
           (set-datetime-error! error-message)))))
    (add-watch
     time-atom
     :time
     (fn [key _ _ new-time]
       (log/trace key @date-atom new-time)
       (let [new-datetime (time/time-to @date-atom
                                        (:hours new-time)
                                        (:minutes new-time))
             error-message (is-invalid? new-datetime)]
         (if (not error-message)
           (do
             (set-date! new-datetime)
             (set-datetime-error! nil))
           (set-datetime-error! error-message)))))
    [:div
     {:class class}
     (datepicker
      {:date date-atom
       :format date-print-format
       :position "top right"
       :context context
       :class "opux-fieldset__inline-item date"})
     (timepicker
      {:time time-atom
       :class "opux-fieldset__inline-item opux-input opux-input--select opux-input--select--time time"})
     (if (rum/react error)
       [:span {:class "error"} (rum/react error)])]))

(defn readable-address [{:keys [street street-number town]}]
  (-> (filter #(not (nil? %)) [street street-number town])
      ((fn [address]
         (if (not (empty? address))
           (let [last-dropped (drop-last address)
                 before-last (last last-dropped)
                 all-but-last-two (drop-last last-dropped)]
             (log/info address)
             (concat all-but-last-two [(str before-last ",") (last address)]))
           [])))
      (#(clojure.string/join " " %1))))

(rum/defc location-selector < address/autocomplete-mixin
  [{:keys [location class placeholder]}]
  [:input
   {:type "text"
    :class (str "autocomplete " class)
    :placeholder placeholder
    :default-value (readable-address @location)}])

(defn multi-choice [tr choice-text-fn selected choice]
  (let [id (str "multi-choice-" (name choice))
        is-selected (> (.indexOf @selected choice) -1)
        class (str "opux-input opux-input--checkbox--multi-select hidden choice " (name choice))]
    (into []
          (concat
           [[:input
             {:class class
              :type "checkbox"
              :id id
              :default-checked is-selected
              :name (name choice)
              :on-change
              (fn [e]
                (let [choice (keyword
                              #?(:clj (get-in e [:target :name])
                                 :cljs (.. e -target -name)))
                      checked #?(:clj (get-in e [:target :checked])
                                 :cljs (.. e -target -checked))
                      selected-set (into #{} @selected)
                      operation (if checked conj disj)]
                  (reset!
                   selected
                   (into [] (operation selected-set choice)))))}]]
           [(label
             [:i {:class (str "opux-icon--multi-select opux-icon-circled opux-icon--multi-select--" (name choice))}]
             {:class "opux-input__label--multi-select"
              :for id}
             (tr [(choice-text-fn choice)]))]))))

(rum/defc multi-selector-for-schema < rum/reactive
  [{:keys [schema context value choice-name-fn error]}]
  (let [tr (:tr context)
        make-multi-choice (partial multi-choice tr choice-name-fn value)
        single (cond
                 (and (not (set? schema))
                      (sequential? schema)
                      (instance? schema.core.One
                                 (first (st/schema-value schema))))
                 (last (first (:schema (first (st/schema-value schema)))))

                 (and (not (set? schema))
                      (sequential? schema))
                 (st/schema-value (first schema))

                 :else (st/schema-value schema))
        all (cond
              (or (set? single) (sequential? single))
              (into [:div {:class "opux-fieldset opux-fieldset--multi-select"}]
                    (mapcat identity (map make-multi-choice single))))]
    all))

(rum/defc max-participants [{:keys [schema value error context] :as params}]
  (let [tr (:tr context)]
    [:div {:class "opux-fieldset__item"}
     (label
      (tr [:pages.quest.edit.max-participants.can-join])
      {:class (class-label-with-error error "max-participants")})
     (input
      (conj params
            {:class "opux-input opux-input--text opux-input--inline"
             :transform-value #(if (string? %) (mangling/parse-natural-number %))}))
     [:span {:class "opux-input__suffix"}
      (tr [:pages.quest.edit.max-participants.amount-of-people])]]))

(defn single-choice [tr current-choice class [key value]]
  (let [group-name (str class "-radio-binary-choice")
        id (str group-name "-" (name key))]
    [:div
     {:class "opux-input__container opux-input__container--radio radio-single-choice"}
     [:input
      {:type "radio"
       :class "opux-input--radio"
       :id id
       :name group-name
       :default-checked (= @current-choice value)
       :on-change
       (fn [] (reset! current-choice value))}]
     (label
      [:span (tr [key])]
      {:class "opux-input_label opux-input__label--radio"
       :for id})]))


(rum/defc radio-binary [{:keys [schema value error class context]} choices]
  (let [tr (:tr context)
        make-single-choice (partial single-choice tr value class)
        html-choices (concat (map make-single-choice choices))]
    (into [:div {:class (str "radio-binary " class)}] html-choices)))

(rum/defc checkbox-binary < rum/reactive
  [{:keys [schema value error class id context]}]
  [:input
   {:type "checkbox"
    :class "opux-input--checkbox"
    :default-checked (rum/react value)
    :id id
    :on-change
    (fn [e]
      (reset! value (checked-from-event e)))}])

(rum/defcs file-input < dropzone-mixin
  [state {:keys [value error]}]
  [:div {:class "dropzone picture-upload"}])

(rum/defc form-section [title & content]
  (let [content-vector (into [] content)
        default-content [:fieldset
                         {:class "opux-fieldset opux-form-section__fieldset"}
                         (cond (not-empty title)
                               [:h3
                                {:class "opux-form-section__title"}
                                title])]]
    [:div {:class "opux-form-section"}
     (into [] (concat default-content content-vector))]))

(rum/defc section [{:keys [title content] :as params}]
  [:section
   [:h2
    {:class "section-title"}
    title]
   content])

(rum/defc header [{:keys [hierarchy tr asset-path] :as context}]
  [:header
   {:class "opux-page-section opux-page-section--header"}
   [:h1
    {:class "opux-logo opux-logo--header"}
    [:a
     {:href (path-for hierarchy :index)}
     (tr [:name])]]
   [:div {:id "top-navigation"}
    (navigation/top-navigation context)]])

(rum/defc body-content [header content scripts]
  [:body header content scripts])

(rum/defc head-content [{:keys [title asset-path]}]
  [:head
   [:title title]
   [:meta {:charset "UTF-8"}]
   [:link {:href (str asset-path "/css/screen.css") :rel "stylesheet" :type "text/css"}]])

(defn script-tag [url]
  [:script
   {:src url :type "text/javascript"}])

(defn app-structure
  [{:keys [context title content csrf-token servlet-context scripts]}]
  (let [tr (:tr context)
        asset-path (:asset-path context)
        default-script (str asset-path "/js/app.js")
        script-tags (vec (map script-tag (conj scripts default-script)))]
    (page
     (head-content {:title (tr [:title] [title]) :asset-path asset-path})
     (body-content
      (header context)
      [:div {:id "app" :class "opux-page-section" :dangerouslySetInnerHTML {:__html content}}]
      (into [:div {:class "script-tags"}] script-tags)
      ))))

(defn wrap-paragraph [content]
  (into [:div]
        (map #(if (not (nil? %)) [:p %] "")
             (string/split content #"\n"))))

(defn append-if-valid [text separator]
  (if (not (nil? text))
    (str ", " text)
    ""))

(defn combine-text [separator first-text & args]
  (if (nil? first-text)
    (if args
      (let [[new-first & rest] args]
        (recur separator new-first rest))
      "")
    (str first-text (string/join (map #(append-if-valid % separator) args)))))
