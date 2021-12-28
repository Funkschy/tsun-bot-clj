(ns tsunbot.core
  (:gen-class)
  (:require [clojure.edn :as edn]
            [clojure.core.async :as a]
            [discljord.connections :as c]
            [discljord.messaging :as m]
            [clojure.tools.logging :as log]

            [tsunbot.commands.impl :as cmd]
            [tsunbot.discord :as discord]))

(def config (edn/read-string (slurp "config.edn")))

(defn -main[& args]
  (let
    [event-ch   (a/chan 100)
     command-ch (a/chan 100)
     dispatcher (future (cmd/dispatcher command-ch event-ch))]

    (future (discord/connect event-ch command-ch config))
    @dispatcher

    (a/close! event-ch)
    (a/close! command-ch)))
