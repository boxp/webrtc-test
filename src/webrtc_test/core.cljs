(ns webrtc-test.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [put! <! timeout]]))

(enable-console-print!)

(println "Edits to this text should show up in your developer console.")

;; define your app data so that it doesn't get over-written on reload

(def api-key "****************")

(defonce app-state (atom {:text "Welcome to WebRTC testing!"
                          :peer-obj nil
                          :peer-id ""
                          :conn-obj nil
                          :another-peer-id ""
                          :log []}))

(defn- conn-listener!
  [data]
  (swap! app-state update :log #(conj % data)))

(defn connect!
  []
  (let [conn (.connect (:peer-obj @app-state) (:another-peer-id @app-state))]
    (swap! app-state assoc :conn-obj conn)))

(defn send!
  [owner]
  (.send (:conn-obj @app-state)
         (.-value (om/get-node owner "new-message"))))

(defcomponent widget [data owner]
  (will-mount [_]
    (let [peer (js/Peer. #js {"key" api-key})]
      (.on peer "open" (fn [id] (swap! app-state assoc :peer-id id)))
      (.on peer "connection" (fn [conn] (swap! app-state assoc :conn-obj conn)))
      (swap! app-state assoc :peer-obj peer)))
  (render [this]
    (html [:div 
            [:div
              [:p (:text data)]
              [:p (str "peer-id is: " (:peer-id data))]
              [:p (str "another-peer-id is: " (:another-peer-id data))]
              [:p (if (:peer-obj data)
                    "Registered"
                    "Not Registered")]
              [:p (if (:conn-obj data)
                    (do (.on (:conn-obj data) "data" conn-listener!)
                        "Connected")
                    "Not Connected")]
              [:button {:on-click (fn [e]
                                    (om/update! data :another-peer-id
                                      (js/prompt "接続先のIDを入力してください。"))
                                    (connect!))}
                       "他のピアに接続"]]
            [:div#log
              [:p [:input {:type "text"
                           :ref "new-message"}]
                  [:button {:on-click (fn [e] (send! owner))}
                           "送信"]]
              [:ul (map #(vector :li %) (reverse (:log data)))]]])))

(om/root widget app-state {:target (. js/document (getElementById "app"))})

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
