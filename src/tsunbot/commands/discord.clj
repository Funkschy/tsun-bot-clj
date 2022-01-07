(ns tsunbot.commands.discord
  (:require [clojure.string :as str]
            [tsunbot.commands.common :as c]))

(defn userid [[username & _] {author-name :username :as state} env]
  (-> (c/get-user-by-name (or username author-name) state) :user :id))
