(ns tsunbot.commands.parse
  (:require [clojure.spec.alpha :as spec]
            [clojure.string :as str]
            [tsunbot.commands.specs :as s]))

(defn parse-single [input]
  (when (spec/valid? :tsunbot/command input)
    (let [[command & args] (remove empty? (str/split input #"\s+"))]
      (apply vector (apply str (drop 1 command)) args))))

(defn parse [input]
  (let [commands (map str/trim (str/split input #"\|"))
        parsed   (take-while (comp not nil?) (map parse-single commands))]
    (when (= (count commands) (count parsed))
      parsed)))
