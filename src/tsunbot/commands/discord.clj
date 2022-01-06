(ns tsunbot.commands.discord
  (:require [clojure.core.async :as a]
            [clojure.string :as str]
            [discljord.messaging :as m]))

; TODO: get-user-id by name should be part of the state functions and passed down
;  commands should not use any of the api's directly

(defn run-sync [f & args]
  (let [result (promise)]
    (a/go (deliver result @(apply f args)))
    @result))

(defn get-user-by-name [username {:keys [message-ch guild-id]}]
  (first (run-sync m/search-guild-members! message-ch guild-id username)))

(defn userid [[username & _] state env]
  (-> (get-user-by-name username state) :user :id))
