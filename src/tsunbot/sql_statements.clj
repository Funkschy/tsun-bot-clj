(ns tsunbot.sql_statements
  (:require [tsunbot.db :as db]))

(defn set-role [userid new-role]
  (let [statement (str "INSERT OR REPLACE INTO "
                       "user (id, role) "
                       "VALUES (?, ?)")]
    (db/exec-update statement userid new-role)))
