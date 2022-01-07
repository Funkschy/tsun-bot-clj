(ns tsunbot.commands.discord
  (:require [clojure.string :as str]
            [tsunbot.user :as user]
            [tsunbot.commands.common :as c]))

(defn userid [[username & _] {author-name :username :as state} env]
  (let [n (or username author-name)]
    (or (-> (c/get-user-by-name n state) :user :id)
        (format (:err-fmt state) n))))

(defn set-role [[username role] {:keys [authorid] :as state} env]
  (if-let [new-role (user/roles (keyword role))]
    (if-let [id (-> (c/get-user-by-name username state) :user :id)]
      (if (= id authorid)
        (:self-set-fmt state)
        (c/set-user-role id new-role state))
      (format (:no-user-fmt state) username))
    (format (:invalid-role-fmt state) role)))
