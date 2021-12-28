(ns tsunbot.commands.specs
  (:require [clojure.string :refer [starts-with?]]
            [clojure.spec.alpha :as s]))

(s/def :tsunbot/command (s/and string? #(starts-with? % "!")))

(def opt-command-config-params [:min-args :max-args])
(s/def :tsunbot/opt-command-config-param (into #{} opt-command-config-params))
