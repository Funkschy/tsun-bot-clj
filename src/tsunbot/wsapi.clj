(ns tsunbot.wsapi
  (:require [clojure.edn :as edn]
            [clojure.core.async :as a]
            [clojure.tools.logging :as log]
            [org.httpkit.server :refer [run-server as-channel send!]]

            [tsunbot.commands.parse :as p]
            [tsunbot.config :refer [start-time]]))

(defn return-data [data]
  (pr-str {:type :data :data data}))

(defmulti handle-message
  (fn [channels {route :route}] route))

(defmethod handle-message "echo" [_ {{value :value} :data}]
  (return-data (str value)))

(defmethod handle-message "start-time" [_ message]
  (return-data (str start-time)))

(defmethod handle-message "raw-command" [{:keys [command-ch message-ch]} {{cmd :cmd} :data}]
  (let [send-msg (fn [msg] (send! message-ch (return-data msg)))]
    (a/put! command-ch {:system :web
                        :commands (p/parse cmd)
                        :send-msg send-msg
                        :reply send-msg
                        :username "admin"
                        :authorid 0}))
  (return-data "waiting..."))

(defn websocket-handler [command-ch request]
  (as-channel request
              {:on-receive (fn [ch message]
                             (->> message
                                  edn/read-string
                                  (handle-message {:command-ch command-ch, :message-ch ch})
                                  (send! ch)))
               :on-close   (fn [ch status]
                             (log/info "disconnected:" status))
               :on-open    (fn [ch]
                             (log/info "connected:" ch)
                             (send! ch (return-data "connected")))}))

(defn start-api [command-ch config]
  (run-server (partial websocket-handler command-ch) {:port (:port config)}))
