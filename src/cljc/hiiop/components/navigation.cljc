(ns hiiop.components.navigation
  (:require [rum.core :as rum]
            [bidi.bidi :refer [path-for]]
            #?(:clj [hiiop.config :refer [env]]
               :cljs [hiiop.client-config :refer [env]])))

(rum/defcs top-navigation < (rum/local false ::is-active)
  [state {:keys [hierarchy tr current-locale identity]}]
  (let [is-active (::is-active state)
        site-base-url (:site-base-url env)]
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
       ;; [:li
       ;;  {:class "opux-menu__item opux-menu__item--main"}
       ;;  [:a
       ;;   {:class "opux-menu__item-link opux-menu__item-link--main"
       ;;    :href "http://tarinat.hiiop100.fi"}
       ;;   (tr [:pages.ideas.title])]]
       [:li
        {:class "opux-menu__item opux-menu__item--main-quest opux-menu__item--main--browse-quest"}
        [:a
         {:class "opux-menu__item-link opux-menu__item-link--main"
          :href (str site-base-url (path-for hierarchy :browse-quests))}
         (tr [:actions.quest.browse])]]
       [:li
        {:class "opux-menu__item opux-menu__item--main--quest opux-menu__item--main--create-quest"}
        [:a
         {:class "opux-menu__item-link opux-menu__item-link--main"
          :href (str site-base-url (path-for hierarchy :create-quest))}
         (tr [:actions.quest.create])]]]
      [:div {:class "opux-menu--right"}

       ;; [:ul
       ;;  {:class "opux-menu opux-menu--languages"}
       ;;  [:li
       ;;   {:class "opux-menu__item opux-menu__item--languages"}
       ;;   [:a
       ;;    {:href "?lang=fi"
       ;;     :class (str
       ;;              "opux-menu__item-link opux-menu__item-link--languages "
       ;;              (when (= current-locale :fi) "opux-menu__item-link opux-menu__item-link--languages is-active"))}
       ;;    "fi"]
       ;;   ]
       ;;  [:li
       ;;   {:class "opux-menu__item opux-menu__item--languages"}
       ;;   [:a
       ;;    {:href "?lang=sv"
       ;;     :class (str
       ;;              "opux-menu__item-link opux-menu__item-link--languages "
       ;;              (when (= current-locale :sv) "opux-menu__item-link opux-menu__item-link--languages is-active"))}
       ;;    "sv"]]]

       [:ul {:class "opux-menu opux-menu--login"}

        [:li {:class "opux-menu__item opux-menu__item--login"}

         (if-not identity
           ;; not logged in
           [:a {:class "opux-menu__item-link opux-menu__item-link--login"
                :href (str site-base-url (path-for hierarchy :login))}
            (tr [:actions.user.login])]

           ;; logged in
           (if (:name identity)
             [:a {:class "opux-menu__item-link opux-menu__item-link--login"
                  :href (str site-base-url (path-for hierarchy :profile))}
              (:name identity)]))]

        [:li {:class "opux-menu__item opux-menu__item--login"}
         [:a {:class "opux-menu__item-link opux-menu__item-link--login"
              :href (path-for hierarchy :profile)}
          [:i {:class "opux-icon-circled opux-icon-person"}]]]]]]]))

(rum/defc footer-navigation
  [{:keys [hierarchy tr current-locale]}]
  (let [locale-string (name (or current-locale :fi))
        blog-url (:hiiop-blog-base-url env)
        static-page-url (str blog-url "/"
                             locale-string
                             "/pages/")]
    [:nav {:class "opux-nav opux-nav--footer"}
     [:ul {:class "opux-menu opux-menu--footer"}

      ;; [:li {:class "opux-menu__item"}
      ;;  [:a {:class "opux-menu__item-link opux-menu__item-link--footer"
      ;;    :href "#"}
      ;;   (tr [:footer.rules-and-guidelines])]]

    [:li {:class "opux-menu__item"}
     [:a {:class "opux-menu__item-link opux-menu__item-link--footer"
          :href (str static-page-url
                     (tr [:footer.faq-page-key])
                     ".html")}
      (tr [:footer.faq])]]

    [:li {:class "opux-menu__item"}
     [:a {:class "opux-menu__item-link opux-menu__item-link--footer"
          :href (str static-page-url
                     (tr [:footer.contact-page-key])
                     ".html")}
      (tr [:footer.contact])]]

    [:li {:class "opux-menu__item"}
     [:a {:class "opux-menu__item-link opux-menu__item-link--footer"
          :href (str static-page-url
                     (tr [:footer.terms-of-service-page-key])
                     ".html")}
      (tr [:footer.terms-of-service])]]

    [:li {:class "opux-menu__item"}
     [:a {:class "opux-menu__item-link opux-menu__item-link--footer"
          :href (str static-page-url
                     (tr [:footer.privacy-page-key])
                     ".html")}
      (tr [:footer.privacy])]]]]))
