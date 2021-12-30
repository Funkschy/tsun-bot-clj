(ns tsunbot.commands.impl
  (:require [clojure.edn :as edn]
            [clojure.spec.alpha :as spec]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [clojure.core.async :as a]

            [tsunbot.commands.general :as general]
            [tsunbot.commands.specs :as s]))

(defn load-commands []
  (log/info "loading commands.edn")
  (edn/read-string (slurp "commands.edn")))

(defn resolve-command [env command]
  ((fn inner [command alias-chain]
     (let [general  (get-in env [:commands :general])
           cmd-info (general (symbol command))]
       (when cmd-info
         (if (= (:type cmd-info) :alias)
           (inner (:command cmd-info) (conj alias-chain command))
           (when-let [function (ns-resolve (:namespace general) (symbol command))]
             [function cmd-info (conj alias-chain command)])))))

   command []))

(def env (atom {:commands        (load-commands)
                :resolve-command resolve-command
                :load-commands   load-commands}))

(defn check-num-args [cmd-info args f k]
  (if (k cmd-info)
    (f (count args) (k cmd-info))
    true))

(defn add-data [cmd-info context]
  (if (:data cmd-info) (conj context (:data cmd-info)) context))

(defn execute-single [[command & args] context]
  (if-let [[function cmd-info] (resolve-command @env command)]
    (if (and (check-num-args cmd-info args >= :min-args)
             (check-num-args cmd-info args <= :max-args))
      (function args (add-data cmd-info context) env)
      {:error  (str "Wrong number of args for " command ": " args)})
    {:error (str "No command called " command)}))

(defn add-args [cmd piped-args]
  (if (nil? piped-args)
    cmd
    (conj cmd piped-args)))

(defn execute [commands {:keys [system username] :as context}]
  (log/info system "command by" username ":" commands)
  (reduce (fn [piped-args cmd]
            (let [command (add-args cmd piped-args)
                  result  (execute-single command context)]
              (if (:error result)
                (reduced result)
                result)))
          nil
          commands))

(defn dispatcher [command-ch event-ch]
  (loop []
    (when-let [{:keys [commands username reply] :as context} (a/<!! command-ch)]
      (if (and (= (first (first commands)) "quit") (= username "Funkschy"))
        (do (a/>!! event-ch [:quit nil])
            (log/info "dispatching quit event"))

        (do (future
              (let [result (execute commands context)]
                (if (:error result)
                  (reply (:error result))
                  (reply result))))
            (recur)))))

  (log/info "quitting dispatcher"))
