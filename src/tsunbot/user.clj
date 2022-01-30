(ns tsunbot.user
  (:require [tsunbot.db :as db]))

(def roles {:none  0
            :mod   1
            :admin 2})

(def rev-roles (clojure.set/map-invert roles))

(defn get-num-role [userid]
  (or (->> (db/exec-query "select role from user where id = ?" userid)
           (first)
           (:role))
      0))

(defn get-role [userid]
  (rev-roles (get-num-role userid)))

(defn has-sufficient-rights [userid min-role]
  (if min-role
    (or (= "0" userid) ; 0 is a special id, which is only valid in the admin interface
        (>= (get-num-role userid) (roles min-role)))
    true))
