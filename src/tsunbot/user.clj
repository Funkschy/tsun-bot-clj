(ns tsunbot.user
  (:require [tsunbot.db :as db]))

(def roles {:none  0
            :mod   1
            :admin 2})

(def rev-roles (clojure.set/map-invert roles))

(defn get-num-role [userid]
  (->> (db/exec-query "select role from user where id = ?" userid)
       (first)
       (:role)))

(defn get-role [userid]
  (rev-roles (get-num-role userid)))

(defn has-sufficient-rights [userid min-role]
  (if min-role
    (>= (get-num-role userid) (roles min-role))
    true))
