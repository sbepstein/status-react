(ns messenger.android.contacts-list
  (:require-macros
   [natal-shell.components :refer [view text image touchable-highlight list-view
                                   toolbar-android]]
   [natal-shell.data-source :refer [data-source clone-with-rows]]
   [natal-shell.core :refer [with-error-view]]
   [natal-shell.alert :refer [alert]])
  (:require [om.next :as om :refer-macros [defui]]
            [re-natal.support :as sup]
            [messenger.state :as state]
            [messenger.android.resources :as res]
            [messenger.android.chat :refer [chat]]))

(def fake-contacts? true)

(def react-native-contacts (js/require "react-native-contacts"))

(defn nav-push [nav route]
  (binding [state/*nav-render* false]
    (.push nav (clj->js route))))

(defn show-chat [nav]
  (nav-push nav {:component chat
                 :name "chat"}))

(defui Contact
  static om/Ident
  (ident [this {:keys [name]}]
         [:contact/by-name name])
  static om/IQuery
  (query [this]
         '[:name :photo :delivery-status :datetime :new-messages-count :online])
  Object
  (render [this]
          (let [{:keys [name photo delivery-status datetime new-messages-count online]}
                (dissoc (om/props this) :om.next/computed)
                {:keys [nav]} (om/get-computed this)]
            (touchable-highlight
             {:onPress (fn [] (show-chat nav))}
             (view {:style {:flexDirection "row"
                            :marginTop 5
                            :marginBottom 5
                            :paddingLeft 15
                            :paddingRight 15
                            :height 75
                            :transform [{:translateX 0}
                                        {:translateY 0}]}}
                   (view {:width 54
                          :height 54}
                         ;;; photo
                         (view {:width 54
                                :height 54
                                :borderRadius 50
                                :backgroundColor "#FFFFFF"
                                :elevation 6}
                               (image {:source (if (< 0 (count photo))
                                                 {:uri photo}
                                                 res/user-no-photo)
                                       :style {:borderWidth 2
                                               :borderColor "#FFFFFF"
                                               :borderRadius 50
                                               :width 54
                                               :height 54
                                               :position "absolute"
                                               ;; :top 2
                                               ;; :right 2
                                               }}))
                         ;;; online
                         (when online
                           (view {:position "absolute"
                                  :top 41
                                  :left 36
                                  :width 12
                                  :height 12
                                  :borderRadius 50
                                  :backgroundColor "#FFFFFF"
                                  :elevation 6}
                                 (image {:source res/online-icon
                                         :style {:width 12
                                                 :height 12}}))))
                   (view {:style {:flexDirection "column"
                                  :marginLeft 7
                                  :marginRight 10
                                  :flex 1
                                  :position "relative"}}
                         ;;; name
                         (text {:style {:fontSize 15
                                        :fontFamily "Avenir-Roman"}} name)
                         ;;; last message
                         (text {:style {:color "#AAB2B2"
                                        :fontFamily "Avenir-Roman"
                                        :fontSize 14
                                        :marginTop 2
                                        :paddingRight 10}}
                               (str "Hi, I'm " name)))
                   (view {:style {:flexDirection "column"}}
                         ;;; delivery status
                         (view {:style {:flexDirection "row"
                                        :position "absolute"
                                        :top 0
                                        :right 0}}
                               (when delivery-status
                                 (image {:source (if (= (keyword delivery-status) :seen)
                                                   res/seen-icon
                                                   res/delivered-icon)
                                         :style {:marginTop 5}}))
                               ;;; datetime
                               (text {:style {:fontFamily "Avenir-Roman"
                                              :fontSize 11
                                              :color "#AAB2B2"
                                              :letterSpacing 1
                                              :lineHeight 15
                                              :marginLeft 5}}
                                     datetime))
                         ;;; new messages count
                         (when (< 0 new-messages-count)
                           (view {:style {:position "absolute"
                                          :right 0
                                          :bottom 24
                                          :width 18
                                          :height 18
                                          :backgroundColor "#6BC6C8"
                                          :borderColor "#FFFFFF"
                                          :borderRadius 50
                                          :alignSelf "flex-end"}}
                                 (text {:style {:width 18
                                                :height 17
                                                :fontFamily "Avenir-Roman"
                                                :fontSize 10
                                                :color "#FFFFFF"
                                                :lineHeight 19
                                                :textAlign "center"
                                                :top 1}}
                                       new-messages-count)))))))))

(def contact (om/factory Contact {:keyfn :name}))

(defn render-row [nav row section-id row-id]
  (contact (om/computed (js->clj row :keywordize-keys true)
                        {:nav nav})))

(defn generate-contact [n]
  {:name (str "Contact " n)
   :photo ""
   :delivery-status (if (< (rand) 0.5) :delivered :seen)
   :datetime "15:30"
   :new-messages-count (rand-int 3)
   :online (< (rand) 0.5)})

(defn generate-contacts [n]
  (map generate-contact (range 1 (inc n))))

(defn load-contacts []
  (if fake-contacts?
    (swap! state/app-state update :contacts-ds
           #(clone-with-rows %
                             (vec (generate-contacts 100))))
    (.getAll react-native-contacts
             (fn [error raw-contacts]
               (when (not error)
                 (let [contacts (map (fn [contact]
                                       (merge (generate-contact 1)
                                              {:name (:givenName contact)
                                               :photo (:thumbnailPath contact)}))
                                     (js->clj raw-contacts :keywordize-keys true))]
                   (swap! state/app-state update :contacts-ds
                          #(clone-with-rows % contacts))))))))

(defui ContactsList
  static om/IQuery
  (query [this]
         '[:contacts-ds])
  Object
  (componentDidMount [this]
                     (load-contacts))
  (render [this]
          (let [{:keys [contacts-ds]} (om/props this)
                {:keys [nav]} (om/get-computed this)]
            (view {:style {:flex 1}}
                  (toolbar-android {:logo res/logo-icon
                                    :title "Chats"
                                    :style {:backgroundColor "#e9eaed"
                                            :height 56}})
                  (list-view {:dataSource contacts-ds
                              :renderRow (partial render-row nav)
                              :style {}})))))

(def contacts-list (om/factory ContactsList))