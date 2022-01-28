(ns tsunbot.discord
  (:require [clojure.core.async :as a]
            [clojure.tools.logging :as log]
            [clojure.string :as str]
            [discljord.connections :as c]
            [discljord.messaging :as m]

            [tsunbot.sql_statements :as sql]
            [tsunbot.commands.specs :as s]
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
    [send-msg (fn [content]
                (doseq [content-chunk (partition-all 2000 content)] ; max message length
                  (m/create-message! message-ch
                                     channel-id
                                     :content (str/join content-chunk))))
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
                                 request-ch))))))

(defmethod handle-event :default [event-type event-data {request-ch :request-ch}]
  (log/info "received unhandled" event-type "discord event"))

(defn fullfill [p f & args]
  (a/go (deliver p @(apply f args)))
  p)

(defrecord DiscordApiWrapper [message-ch]
  s/ApiWrapper

  (get-user-by-name [this {:keys [res-promise guild-id username]}]
    (fullfill res-promise m/search-guild-members! message-ch guild-id username))

  (set-user-role [this {:keys [userid role]}]
    (sql/set-role userid role)))

(defn dispatch-request [{:keys [request-ch message-ch db-conn]}]
  (let [api-wrapper (DiscordApiWrapper. message-ch)]
    (loop []
      (let [{:keys [method] :as req} (a/<!! request-ch)]
        (method api-wrapper req))
      (recur))))

(defn connect [command-ch {token :token intents :intents}]
  (let
    [event-ch      (a/chan 100)
     connection-ch (c/connect-bot! token event-ch :intents intents)
     message-ch    (m/start-connection! token)
     request-ch    (a/chan 100)
     state         (create-map command-ch message-ch connection-ch request-ch)]

    (try
      (a/go (dispatch-request state))

      (loop []
        (let [[event-type event-data] (a/<!! event-ch)]
          (handle-event event-type event-data state)
          (recur)))

      (catch Exception e
        (log/error e))
      (finally
        (log/info "disconnecting Discord")
        (a/close! request-ch)
        (c/disconnect-bot!  connection-ch)
        (m/stop-connection! message-ch)
        (a/close! event-ch)))))

