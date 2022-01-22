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
    [command-ch (a/chan 100)
     dispatcher (a/go (cmd/dispatcher command-ch))]

    (a/go (discord/connect command-ch (:discord config)))

    (a/<!! dispatcher)

    (db/disconnect!)
    (a/close! command-ch)))
