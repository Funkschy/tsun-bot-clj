(ns tsunbot.commands.specs
  (:require [clojure.string :refer [starts-with?]]
            [clojure.spec.alpha :as s]))

(s/def :tsunbot/command (s/and string? #(starts-with? % "!")))

(defprotocol ApiWrapper
  "A wrapper for a platform specific api"
  (get-user-by-name [this req]
                    "Get a (platform specific) user object from the username")
  (set-user-role [this req]
                    "Set the role of a user"))

(def opt-command-config-params [:tsunbot/min-args :max-args :data :needs :min-role])
(def req-command-config-params [:type :help])
