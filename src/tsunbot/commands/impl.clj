(ns tsunbot.commands.impl
  (:require [clojure.edn :as edn]
            [clojure.spec.alpha :as spec]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [clojure.core.async :as a]

            [tsunbot.commands.general :as general]
            [tsunbot.commands.specs :as s]))

(def commands (edn/read-string (slurp "commands.edn")))

(defn resolve-command [command]
  ((fn inner [command alias-chain]
     (when-let [cmd-info    ((:general commands) (symbol command))]
       (if (= (:type cmd-info) :alias)
         (inner (:command cmd-info) (conj alias-chain command))
         (when-let [function (ns-resolve (:namespace (:general commands)) (symbol command))]
           [function cmd-info (conj alias-chain command)]))))

   command []))

(def env {:commands        commands
          :resolve-command resolve-command})

(defn check-num-args [cmd-info args f k]
  (if (k cmd-info)
    (f (count args) (k cmd-info))
    true))

(defn select-relevant [m ks]
  (if ks (select-keys m ks) {}))

(defn add-data [cmd-info context]
  (if (:data cmd-info) (conj context (:data cmd-info)) context))

(defn execute-single [[command & args] context]
  (when-let [[function cmd-info] (resolve-command command)]
    (if (and (check-num-args cmd-info args >= :min-args)
             (check-num-args cmd-info args <= :max-args))
      (function args
                (add-data cmd-info context)
                (select-relevant env (:needs cmd-info)))
      {:error  (str "Wrong number of args for " command ": " args)})))

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
