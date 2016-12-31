(ns hiiop.components.part-party
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require [rum.core :as rum]
            [taoensso.timbre :as log]
            [bidi.bidi :refer [path-for]]
            #?(:cljs [hiiop.client-api :as api])
            [hiiop.url :refer [redirect-to]]
            [hiiop.routes.page-hierarchy :refer [hierarchy]]
            [hiiop.html :as html]))


(rum/defc part
  [{:keys [party-member quest view context]}]
  (log/info "part")
  (let [tr (:tr context)]
    [:div {:class "opux-content opux-content--small"}
     [:h2 {:class "opux-centered"} (tr [:pages.quest.part.title])]
     [:form
      {:class "opux-form opux-content opux-content--small"
       :on-submit
       (fn [e]
         (.preventDefault e)
         #?(:cljs
            (go
              (reset! view "processing")
              (if (api/remove-party-member party-member)
                (reset! view "success")
                (reset! view "fail")))
            ))}
      [:div {:class "opux-section"}
       [:p {:class "opux-centered"}
        (tr [:pages.quest.part.text] [(:name quest)])]
       [:div
        {:class "opux-fieldset__item opux-fieldset__item--inline-container"}
        (html/button
         (tr [:actions.cancel])
         {:class "opux-button opux-form__button opux-button--dull opux-fieldset__inline-item"
          :on-click
          (fn []
            (redirect-to
             {:path-key :quest
              :with-params [:quest-id (:quest-id party-member)]})
            )})
        (html/button
         (tr [:pages.quest.part.confirm])
         {:type "submit"
          :class "opux-button opux-form__button opux-button--highlight opux-fieldset__inline-item"})]]]]))

(rum/defc processing < rum/reactive []
  [:div {:class "opux-card__overlay"}
   [:div {:class "opux-content opux-centered"}
    [:i {:class "opux-icon opux-icon-ellipses"}]]])

(rum/defc success < rum/reactive
  [{:keys [context quest]}]
  (let [tr (:tr context)]
    [:div {:class "opux-section"}
     [:h2
      {:class "opux-centered"}
      (tr [:pages.quest.part.success])]
     [:p
      {:class "opux-content opux-centered"}
      [:a
       {:href (path-for hierarchy :quest :quest-id (:id quest))
        :class "opux-button opux-button--highlight"}
       (tr [:pages.quest.part.to-quest])]]]))

(rum/defc fail < rum/reactive
  [{:keys [context quest]}]
  (let [tr (:tr context)]
    [:div {:class "opux-section"}
    [:h2
     {:class "opux-centered"}
     (tr [:pages.quest.part.fail])]
     [:p
      {:class "opux-content opux-centered"}
      [:a
       {:href (path-for hierarchy :quest :quest-id (:id quest))
        :class "opux-button opux-button--highlight"}
       (tr [:pages.quest.part.to-quest])]]]))

(rum/defcs part-party < rum/reactive
                        (rum/local "part" ::view)
  [state {:keys [context quest party-member]}]
  (let [view (::view state)]
    [:div {:class "opux-section"}
     (cond
       (= "part"       @view)
       (part {:context context
              :quest quest
              :party-member party-member
              :view view})
       (= "processing" @view)
       (processing)
       (= "success"    @view)
       (success {:context context
                 :quest quest})
       (= "fail"       @view)
       (fail {:context context
              :quest quest})
    )]))

