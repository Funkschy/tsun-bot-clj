(ns tsunbot.commands.general
  (:require [clojure.string :as str]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]

            [tsunbot.config :refer [config]]
            [tsunbot.http :as http]
            [tsunbot.commands.specs :as s]
            [tsunbot.lib.anime :as anime])
  (:import [java.io File]
           [java.nio.file Files Path LinkOption]))

(defn map-join [separator mapper coll]
  (str/join separator (remove nil? (map mapper coll))))

(defn prefix-if [s prefix pred]
  (if (pred s) (str prefix s) s))

(defn help-single [command state env]
  (let [resolve-command          (:resolve-command env)
        [_ cmd-info alias-chain] (resolve-command (:commands env) command)
        aliases                  (str/join " -> " alias-chain)]
    (if cmd-info
      (str "(" aliases "): " (:help cmd-info))
      (format (:err-fmt state) command))))

(defn help-all [env]
  (letfn
    [(collect-optional [command k]
       (when (command k) (str (name k) ": " (k command))))

     (collect-command [[n command]]
       (when (= (:type command) :command)
         (str (name n)
              ": "
              (:help command)
              (prefix-if
                (map-join ", " (partial collect-optional command) s/opt-command-config-params)
                \newline
                (partial not= "")))))

     (collect-ns [[n commands]]
       (str (name n)
            ":"
            \newline
            (map-join "\n\n" collect-command (filter (comp symbol? first) commands))))]
    (str \newline (map-join "\n-----\n" collect-ns (:commands env)))))

(defn help [[command & _] state env]
  (if command
    (help-single command state env)
    (help-all env)))

(defn cycle-upper-lower [args state env]
  (apply str
         (map-indexed #(if (even? %1)
                         (Character/toLowerCase %2)
                         (Character/toUpperCase %2))
                      (apply str args))))

(defn ping [args state env]
  "pong")

(defn anime [[username & _] state env]
  (let [username     (or username (:username state))
        animelist    (anime/fetch-mal-completed username)
        random-anime (first (shuffle (filter #(> (% "score") 7) animelist)))]
    (if random-anime
      (format (:succ-fmt state) username (random-anime "title") (random-anime "score"))
      (format (:err-fmt state) username))))

(defn anime-backlog [[username & _] state env]
  (let [username (or username (:username state))
        backlog  (anime/fetch-behind-schedule username)]
    (cond
      (not-empty backlog)
      (str/join \newline (map #(format (:succ-fmt state) (% "title") (% :behind)) backlog))

      backlog (format (:no-anime-fmt state) username)
      :else   (format (:err-fmt state) username))))

(defn mal-diff [[username-1 username-2 & _] state env]
  (let [users    (if (nil? username-2) [(:username state) username-1] [username-1 username-2])
        max-diff (apply anime/find-max-diff-anime users)
        scores   (flatten (into [] (:scores max-diff)))]
    (if max-diff
      (apply format (:succ-fmt state) (:title max-diff) scores)
      (:err-fmt state))))

(defn reload-commands [args state env]
  ((:reload-commands env))
  (:succ-fmt state))

(defn logs [args state env]
  (let [file-pattern    (get-in config [:log :file])
        pattern-re      (re-pattern (str file-pattern #"(\.\d+)?"))
        parent-dir      (.. (File. file-pattern) getAbsoluteFile getParentFile toPath)
        grandparent-dir (.getParent parent-dir)
        is-log-file?    (fn [^Path p]
                          (re-matches pattern-re (.toString (.relativize grandparent-dir p))))
        last-modified   (fn [^Path p] (Files/getLastModifiedTime p (make-array LinkOption 0)))]
    (->> (Files/list parent-dir)
         .iterator
         iterator-seq
         (filter is-log-file?)
         (sort-by last-modified)
         (map (fn [^Path p] (.. p toAbsolutePath toString)))
         (map slurp)
         flatten
         str/join)))
