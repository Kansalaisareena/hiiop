(ns hiiop.components.address-autocomplete
  (:require [rum.core :as rum]
            [taoensso.timbre :as log]
            [hiiop.mangling :refer [parse-int]]))

(def google-keys->readable
  {:street_number               {:key :street-number :transform parse-int}
   :route                       {:key :street :transform identity}
   :postal_code                 {:key :postal-code :transform identity}
   :locality                    {:key :town :transform identity}
   :country                     {:key :country :transform identity}
   :administrative_area_level_3 {:key :town :transform identity}})

(defn set-location! [location-atom results]
  (swap! location-atom (fn [_ _ _ _] results)))

(defn transform-to [detail]
  ((keyword (first (:types detail))) google-keys->readable))

(defn address-details [address-components]
  (log/trace address-components)
  (let [details
        (map (fn [detail]
               (let [transform-map (transform-to detail)
                     readable-key (:key transform-map)
                     transform-fn (:transform transform-map)]
                 (if readable-key
                   {readable-key (transform-fn (:long_name detail))}
                   {})))
             address-components)]
    (log/trace details)
    (reduce conj details)))

(defn coordinates [location]
  (when location
    {:latitude (str (.lat location))
     :longitude (str (.lng location))}))

(defn get-location-details [place-result]
  (conj
   {:google-place-id (:place_id place-result)
    :coordinates (coordinates (get-in place-result [:geometry :location]))
    :google-maps-url (:url place-result)}
   (address-details (:address_components place-result))))

(def autocomplete-mixin
  {:did-mount
   (fn [state]
     (let [args (first (:rum/args state))
           location (:location args)
           set-location-to! (partial set-location! location)
           Autocomplete #?(:cljs
                           (.. js/google -maps -places -Autocomplete)
                           :clj
                           nil)
           instance #?(:cljs
                           (Autocomplete.
                            (rum/dom-node state)
                            (clj->js {:types ["address"]}))
                           :clj nil)
           place-changed
           (fn []
             #?(:cljs
                (let [js-place (.getPlace instance)
                      cljs-place (js->clj js-place :keywordize-keys true)
                      details (get-location-details cljs-place)]
                  (set-location-to! details))))]
       #?(:cljs (.addListener instance "place_changed" place-changed))
       (assoc state
              ::instance instance)))})
