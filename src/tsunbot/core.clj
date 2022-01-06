(ns tsunbot.core
  (:gen-class)
  (:require [clojure.edn :as edn]
            [clojure.core.async :as a]
            [clojure.tools.logging :as log]

            [tsunbot.db :as db]
            [tsunbot.commands.dispatch :as cmd]
            [tsunbot.discord :as discord]))

(def config (edn/read-string (slurp "config.edn")))

(defn -main[& args]
  (let
    [event-ch   (a/chan 100)
     command-ch (a/chan 100)
     dispatcher (future (cmd/dispatcher command-ch event-ch))]

    (future (discord/connect event-ch command-ch (:discord config)))
    @dispatcher

    (db/disconnect!)
    (a/close! event-ch)
    (a/close! command-ch)))
