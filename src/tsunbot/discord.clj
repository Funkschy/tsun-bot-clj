(ns tsunbot.discord
  (:require [clojure.core.async :as a]
            [discljord.connections :as c]
            [discljord.messaging :as m]
            [clojure.tools.logging :as log]

            [tsunbot.commands.parse :as p]))

(defmacro create-map [& syms]
  (zipmap (map keyword syms) syms))

(defmulti handle-event
  (fn [event-type event-data state] event-type))

(defmethod handle-event :message-create
  [event-type
   {{username :username, bot :bot, authorid :id, :as author} :author
    :keys [channel-id content guild-id]}
   {:keys [message-ch command-ch connection-ch request-ch]}]
  (let
    [send-msg (fn [content] (m/create-message! message-ch channel-id :content content))
     reply    (fn [content] (send-msg (str "<@" authorid "> " content)))
     commands (p/parse content)]
    (when (and (not bot) commands)
      (a/put! command-ch
              (merge {:system :discord}
                     (create-map commands
                                 send-msg
                                 reply
                                 authorid
                                 username
                                 guild-id
                                 message-ch))))))

(defmethod handle-event :default [event-type event-data {request-ch :request-ch}]
  (log/info "received unhandled" event-type "discord event"))

(defn dispatch-discord-event [state event-type event-data]
  (handle-event event-type event-data state))

(defn connect [event-ch command-ch {token :token intents :intents}]
  (let
    [connection-ch (c/connect-bot! token event-ch :intents intents)
     message-ch    (m/start-connection! token)
     state         (create-map command-ch message-ch connection-ch)]

    (try
      (loop []
        (let [[event-type event-data] (a/<!! event-ch)]
          (when-not (= :quit event-type)
            (dispatch-discord-event state event-type event-data)
            (recur))))
      (catch Exception e
        (log/error e))
      (finally
        (log/info "disconnecting Discord")
        (c/disconnect-bot!  connection-ch)
        (m/stop-connection! message-ch)))))

