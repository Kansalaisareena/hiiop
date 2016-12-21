(ns hiiop.components.navigation
  (:require [rum.core :as rum]
            [bidi.bidi :refer [path-for]]))

(rum/defcs top-navigation < (rum/local false ::is-active)
  [state {:keys [hierarchy tr current-locale]}]
  (let [is-active (::is-active state)]
    [:div
     [:div {:class "opux-mobile-hamburger"}
      [:div {:class
             (str "opux-mobile-hamburger__button"
                  (cond @is-active " is-active"))
             :on-click (fn [_]
                         (swap! is-active not))}
       [:div {:class "opux-mobile-hamburger-dash"}]]]
     [:nav
      {:class
       (str "opux-nav opux-nav--header"
            (cond @is-active " is-active"))}
      [:ul
       {:class "opux-menu opux-menu--main"}
       [:li
        {:class "opux-menu__item opux-menu__item--main"}
        [:a
         {:class "opux-menu__item-link opux-menu__item-link--main"
          :href "http://tarinat.hiiop100.fi"}
         (tr [:pages.ideas.title])]]
       [:li
        {:class "opux-menu__item opux-menu__item--main-quest opux-menu__item--main--browse-quest"}
        [:a
         {:class "opux-menu__item-link opux-menu__item-link--main"
          :href (path-for hierarchy :browse-quests)}
         (tr [:actions.quest.browse])]]
       [:li
        {:class "opux-menu__item opux-menu__item--main--quest opux-menu__item--main--create-quest"}
        [:a
         {:class "opux-menu__item-link opux-menu__item-link--main"
          :href (path-for hierarchy :create-quest)}
         (tr [:actions.quest.create])]]]
      [:ul
       {:class "opux-menu opux-menu--languages"}
       [:li
        {:class "opux-menu__item opux-menu__item--languages"}
        [:a
         {:href "?lang=fi"
          :class (str
                  "opux-menu__item-link opux-menu__item-link--languages "
                  (when (= current-locale :fi) "opux-menu__item-link opux-menu__item-link--languages is-active"))}
         "fi"]
        ]
       [:li
        {:class "opux-menu__item opux-menu__item--languages"}
        [:a
         {:href "?lang=sv"
          :class (str
                  "opux-menu__item-link opux-menu__item-link--languages "
                  (when (= current-locale :sv) "opux-menu__item-link opux-menu__item-link--languages is-active"))}
         "sv"]]]
      [:ul
       {:class "opux-menu opux-menu--login"}
       [:li
        {:class "opux-menu__item opux-menu__item--login"}
        [:a
         {:class "opux-menu__item-link opux-menu__item-link--login"
          :href (path-for hierarchy :login)}
         (tr [:actions.user.login])
         ]]
       [:li
        {:class "opux-menu__item opux-menu__item--login"}
        [:a
         {:class "opux-menu__item-link opux-menu__item-link--login"
          :href (path-for hierarchy :user)}
         [:i {:class "opux-icon-circled opux-icon-person"}]]]]]]))