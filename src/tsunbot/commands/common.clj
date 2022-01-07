(ns tsunbot.commands.common
  (:require [clojure.core.async :as a]
            [tsunbot.commands.specs :as s]))

(defn request-sync [request-ch method & args]
  (let [result (promise)]
    (a/>!! request-ch
           (conj {:method method
                  :res-promise result}
                 (apply hash-map args)))
    @result))

(defn set-user-role [id new-role {:keys [request-ch]}]
  (a/>!! request-ch {:method s/set-user-role,
                     :userid id,
                     :role new-role}))

(defn get-user-by-name [username {:keys [request-ch guild-id]}]
  (first (request-sync request-ch
                       s/get-user-by-name
                       :guild-id guild-id
                       :username username)))

