(ns hiiop.components.navigation
  (:require [rum.core :as rum]
            [bidi.bidi :refer [path-for]]
            #?(:clj [hiiop.config :refer [env]]
               :cljs [hiiop.client-config :refer [env]])
            [taoensso.timbre :as log]))

(defn- lang-switcher [{:keys [is-static-page hiiop-blog-base-url current-locale]}]
  [:ul
   {:class "opux-menu opux-menu--languages"}
   [:li
    {:class "opux-menu__item opux-menu__item--languages"}
    [:a
     {:href (if is-static-page
              (str hiiop-blog-base-url
                   "/fi/blog/index.html")
              "?lang=fi")
      :class (str
              "opux-menu__item-link opux-menu__item-link--languages"
              (when (= current-locale :fi)
                " is-active"))}
     "fi"]]
   
   [:li
    {:class "opux-menu__item opux-menu__item--languages"}
    [:a
     {:href (if is-static-page
              (str hiiop-blog-base-url
                   "/sv/blog/index.html")
              "?lang=sv")
      :class (str
              "opux-menu__item-link opux-menu__item-link--languages"
              (when (= current-locale :sv)
                " is-active"))}
     "sv"]]])

(defn- login-menu [{:keys [identity site-base-url active? is-static-page hierarchy tr lang-param]}]
  [:ul {:class "opux-menu opux-menu--login"}

   [:li {:class "opux-menu__item opux-menu__item--login"}
    (if-not identity
      ;; not logged in
      (if-not is-static-page
        ;; hide username on static pages since we don't have the session
        ;; info there. (static pages are on a different domain)
        [:a {:class (str "opux-menu__item-link opux-menu__item-link--login" (active? :login))
             :href (str site-base-url (path-for hierarchy :login))}
         (tr [:actions.user.login])])

      ;; logged in
      (if (:name identity)
        [:a {:class (str "opux-menu__item-link opux-menu__item-link--login" (active? :profile))
             :href (str site-base-url
                        (path-for hierarchy :profile)
                        lang-param)}
         (:name identity)]))]

   [:li {:class "opux-menu__item opux-menu__item--login"}
    [:a {:class (str "opux-menu__item-link opux-menu__item-link--login"
                     (if identity
                       (active? :profile)
                       (active? :login)))
         :href (str site-base-url
                    (path-for hierarchy (if identity :profile :login)))}
     [:i {:class "opux-icon-circled opux-icon-person"}]]]])

(defn- main-menu [{:keys [tr is-active active? hierarchy lang-param site-base-url hiiop-blog-base-url current-locale]}]
  [:ul
   {:class "opux-menu opux-menu--main"}
   [:li
    {:class "opux-menu__item opux-menu__item--main"}
    [:a
     {:class "opux-menu__item-link opux-menu__item-link--main"
      :href (str hiiop-blog-base-url "/"
                 (name current-locale)
                 "/blog/index.html")}
     (tr [:pages.ideas.title])]]
   [:li
    {:class "opux-menu__item opux-menu__item--main-quest opux-menu__item--main--browse-quest"}
    [:a
     {:class (str "opux-menu__item-link opux-menu__item-link--main"
                  (active? :browse-quests))
      :href (str site-base-url
                 (path-for hierarchy :browse-quests)
                 lang-param)}
     (tr [:actions.quest.browse])]]
   [:li
    {:class "opux-menu__item opux-menu__item--main--quest opux-menu__item--main--create-quest"}
    [:a
     {:class (str "opux-menu__item-link opux-menu__item-link--main"
                  (active? :create-quest))
      :href (str site-base-url
                 (path-for hierarchy :create-quest)
                 lang-param)}
     (tr [:actions.quest.create])]]])

(rum/defcs top-navigation < (rum/local false ::is-active)
  [state {:keys [hierarchy tr current-locale identity is-static-page path-key hiiop-blog-base-url]}]
  (let [is-active (::is-active state)
        site-base-url (:site-base-url env)
        hiiop-blog-base-url (or hiiop-blog-base-url
                                (:hiiop-blog-base-url env))
        active? (fn [p] (if (= path-key p) " is-active"))
        lang-param (when is-static-page
                     (str "?lang=" (name current-locale)))]

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
      (main-menu {:is-active is-active
                  :active? active?
                  :hierarchy hierarchy
                  :current-locale current-locale
                  :lang-param lang-param
                  :hiiop-blog-base-url hiiop-blog-base-url
                  :site-base-url site-base-url
                  :tr tr})
      
      [:div {:class "opux-menu--right"}
       
       (lang-switcher {:is-static-page is-static-page
                       :hiiop-blog-base-url hiiop-blog-base-url
                       :current-locale current-locale})
       
       (login-menu {:identity identity
                    :site-base-url site-base-url
                    :is-static-page is-static-page
                    :active? active?
                    :lang-param lang-param
                    :hierarchy hierarchy
                    :tr tr})]]]))

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
          :target "_blank"
          :href (str static-page-url
                     (tr [:footer.faq-page-key])
                     ".html")}
      (tr [:footer.faq])]]

    [:li {:class "opux-menu__item"}
     [:a {:class "opux-menu__item-link opux-menu__item-link--footer"
          :target "_blank"
          :href (str static-page-url
                     (tr [:footer.contact-page-key])
                     ".html")}
      (tr [:footer.contact])]]

    [:li {:class "opux-menu__item"}
     [:a {:class "opux-menu__item-link opux-menu__item-link--footer"
          :target "_blank"
          :href (str static-page-url
                     (tr [:footer.terms-of-service-page-key])
                     ".html")}
      (tr [:footer.terms-of-service])]]

    [:li {:class "opux-menu__item"}
     [:a {:class "opux-menu__item-link opux-menu__item-link--footer"
          :target "_blank"
          :href (str static-page-url
                     (tr [:footer.privacy-page-key])
                     ".html")}
      (tr [:footer.privacy])]]]]))
