(ns hiiop.html
  (:require [rum.core :as rum]
            [taoensso.timbre :as log]
            [schema.core :as s]
            #?(:cljs [schema.core :refer [EnumSchema]])
            [schema-tools.core :as st]
            [schema-tools.coerce :as stc]
            [bidi.bidi :refer [path-for]]
            [hiiop.components.core :as c]
            [hiiop.components.pikaday :refer [pikaday pikaday-mixin]]
            [hiiop.time :as time]
            [hiiop.schema :as hs]
            [hiiop.mangling :as mangling])
  #?(:clj (:import [schema.core EnumSchema])))

(rum/defc page [head body]
  [:html head body])

(rum/defc top-navigation [{:keys [hierarchy tr]}]
  [:nav
   [:ul
    {:class "navigation"}
    [:li
     {:class "stories"}
     [:a
      {:href "http://tarinat.hiiop100.fi"}
      (tr [:pages.ideas.title])]]
    [:li
     {:class "quest browse-quests"}
     [:a
      {:href (path-for hierarchy :browse-quests)}
      (tr [:actions.quest.browse])]]
    [:li
     {:class "quest create-quest"}
     [:a
      {:href (path-for hierarchy :create-quest)}
      (tr [:actions.quest.create])]]]
   [:ul
    {:class "languages"}
    [:li
     [:a
      {:href "?lang=fi"}
      "fi"]
     ]
    [:li
     [:a
      {:href "?lang=sv"}
      "sv"
      ]
     ]]
   [:ul
    {:class "login"}
    [:li
     [:a
      {:href "#kirjaudu"}
      (tr [:actions.user.login])
      ]]]])

(defn class-label-with-error [error class]
  (if (and error (deref error))
    (str class " error")
    (if class
      (str class)
      "")))

(defn class-error-or-hide [error]
  (if (and error (deref error))
    (str "error " (name (deref error)))
    "hide"))

(defn save-value-or-error [coercer value error new-value]
  (let [value-or-error (hs/either coercer new-value)
        coerced-value (:--value value-or-error)
        coerced-error (:--error value-or-error)]
    (if coerced-value (swap! value (fn [old new] coerced-value)))
    (swap! error (fn [_ _] coerced-error))))

(rum/defc label [text {:keys [for class error] :as or-content} & content]
  (let [also-content (if (sequential? or-content) or-content [])
        more-content (if content (into [] content) [])
        content-vector (if (and content (sequential? or-content))
                         (into [] (concat [content] [also-content]))
                         content)
        default-content [:label
                         {:class (class-label-with-error error class)
                          :for for}
                         text]]
    (into [] (concat default-content content-vector))))

(defn value-from-event [e & with-transform]
  (let [transform (or (first with-transform) identity)]
    (-> #?(:cljs (.. e -target -value)
           :clj (get-in e [:target :value]))
        (transform))))

(defn checked-from-event [e]
  (-> #?(:cljs (.. e -target -checked)
         :clj (get-in e [:target :checked]))))


(rum/defc input < rum/reactive
  [{:keys [type value schema matcher error class transform-value context error-key]}]
  (let [tr (:tr context)
        usable-matcher (if (not matcher) {schema #(identity %)} matcher)
        usable-transform (if (not transform-value) #(identity %) transform-value)
        coercer (stc/coercer schema usable-matcher error-key)
        save-val-or-error (partial save-value-or-error coercer value error)]
    [:div
     {:class "input-container"}
     [:input
      {:type "text"
       :class class
       :default-value (rum/react value)
       :on-change
       (fn [e]
         (-> (value-from-event e usable-transform)
             (save-val-or-error)))}]
     [:span
      {:class (class-error-or-hide error)}
      (if (rum/react error) (tr [(rum/react error)]))]]))

(rum/defc text < rum/reactive
  [{:keys [label type value schema matcher error class context error-key]}]
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
  [{:keys [value options transform error]}]
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
       {:default-value (rum/react value)
        :on-change
        (fn [e]
          (let [event-value (value-from-event e usable-transform)]
            (swap! value (fn [old new] event-value))))}]
      html-options))))

(rum/defcs datepicker < pikaday-mixin
  [state {:keys [date min-date max-date format schema error class transform-value context error-key]}]
  [:input
   {:type "text"
    :class class
    :default-value (time/to-string @date format)}])

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
       (swap!
        time
        (fn [_ _ _ _] new-time))))
    (select
     {:options options
      :value select-value-atom
      :transform time/string->time})))

(defn set-error! [error to]
  (swap!
   error
   (fn [_ _ _ _]
     to)))

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
                             (time/is-before? (min-date-object) new-date)))
        is-before-max? (fn [new-date]
                       (or (not max-date)
                           (time/is-after? (max-date-object) new-date)))
        date-object (time/from-string @date value-format)
        date-atom (atom date-object)
        time-atom (atom (time/datetime->time date-object))
        print-date-time-format (str date-print-format " " time-print-format)
        set-date! (fn [new-datetime]
                    (swap!
                     date
                     (fn [_ _ _]
                       (time/to-string new-datetime value-format))))
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
       :class "date"})
     (timepicker
      {:time time-atom
       :class "time"})
     (if (rum/react error)
       [:span {:class "error"} (rum/react error)])]))

(defn multi-choice [tr choice-text-fn selected choice]
  (let [id (str "multi-choice-" (name choice))
        is-selected (> (.indexOf @selected choice) -1)
        class (str "choice " (name choice))]
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
                  (swap!
                   selected
                   (fn [_ _ _ _]
                     (into [] (operation selected-set choice))))))}]]
           [(label
             (tr [(choice-text-fn choice)])
             {:class class
              :for id})]))))

(rum/defc multi-selector-for-schema [{:keys [schema context value]}]
  (let [tr (:tr context)
        make-multi-choice (partial multi-choice tr hs/category-choice value)
        single (cond
                 (and (not (set? schema)) (sequential? schema)) (st/schema-value (first schema))
                 :else (st/schema-value schema))
        all (cond
              (or (set? single) (sequential? single))
              (into [:div {:class "multi-selector"}] (mapcat identity (map make-multi-choice single))))]
    all))

(rum/defc max-participants [{:keys [schema value error context] :as params}]
  (let [tr (:tr context)]
    (label
     (tr [:pages.quest.edit.max-participants.can-join])
     {:class (class-label-with-error error "max-participants")}
     (input
      (conj params
            {:transform-value #(if (string? %) (mangling/parse-int %))}))
     (tr [:pages.quest.edit.max-participants.amount-of-people]))))

(defn single-choice [tr current-choice class [key value]]
  (let [group-name (str class "-radio-binary-choice")
        id (str group-name "-" (name key))]
    [:div
     {:class "radio-single-choice"}
     [:input
      {:type "radio"
       :id id
       :name group-name
       :default-checked (= @current-choice value)
       :on-change
       (fn [] (swap! current-choice (fn [_ _ _ _] value)))}]
     (label
      (tr [key])
      {:for id})]))


(rum/defc radio-binary [{:keys [schema value error class context]} choices]
  (let [tr (:tr context)
        make-single-choice (partial single-choice tr value class)
        html-choices (concat (map make-single-choice choices))]
    (into [:div {:class (str "radio-binary " class)}] html-choices)))

(rum/defc checkbox-binary < rum/reactive
  [{:keys [schema value error class id context]}]
  [:input
   {:type "checkbox"
    :default-checked (rum/react value)
    :id id
    :on-change
    (fn [e]
      (swap! value (fn [_ _ _ _] (checked-from-event e))))}])

(rum/defc form-section [title & content]
  (let [content-vector (into [] content)
        default-content [:fieldset
                         [:h3
                          {:class "form-section"}
                          title]]]
    (into [] (concat default-content content-vector))))

(rum/defc section [{:keys [title content] :as params}]
  [:section
   [:h2
    {:class "section-title"}
    title]
   content])

(rum/defc header [{:keys [hierarchy tr asset-path] :as context}]
  [:header
   [:h1
    {:class "name"}
    [:a
     {:href (path-for hierarchy :index)}
     (tr [:name])]]
   (top-navigation context)])

(rum/defc body-content [header content scripts]
  [:body header content scripts])

(rum/defc head-content [{:keys [title asset-path]}]
  [:head
   [:title title]
   [:meta {:charset "UTF-8"}]
   [:link {:href (str asset-path "/css/screen.css") :rel "stylesheet" :type "text/css"}]])

(defn app-structure [{:keys [context title content csrf-token servlet-context]}]
  (let [tr (:tr context)
        asset-path (:asset-path context)]
    (page
     (head-content {:title (tr [:title] [title]) :asset-path asset-path})
     (body-content
      (header context)
      [:div {:id "app" :class "app" :dangerouslySetInnerHTML {:__html content}}]
      [:div
       [:script {:src (str asset-path "/js/app.js") :type "text/javascript"}]
       [:script {:src "https://cdnjs.cloudflare.com/ajax/libs/moment-timezone/0.5.10/moment-timezone-with-data-2010-2020.min.js" :type "text/javascript"}]]))))
