(ns tsunbot.config
  (:require [clojure.edn :as edn])
  (:import  java.time.LocalDateTime))

(def start-time (LocalDateTime/now))
(def config (edn/read-string (slurp "config.edn")))
