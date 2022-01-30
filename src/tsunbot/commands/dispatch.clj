(ns tsunbot.commands.dispatch
  (:require [clojure.edn :as edn]
            [clojure.spec.alpha :as spec]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [clojure.core.async :as a]

            [tsunbot.user :as user]
            [tsunbot.commands.general]
            [tsunbot.commands.discord]
            [tsunbot.commands.specs :as s]))

(defn load-commands []
  (log/info "loading commands.edn")
  (edn/read-string (slurp "commands.edn")))

(defn lookup-command-in [commands command system]
  (let [sys (system commands)
        cmd (sys command)]
    (when cmd
      [cmd system sys])))

(defn lookup-command [commands command]
  (let [systems (keys commands)
        lookup  (partial lookup-command-in commands command)]
    (into {} (map (fn [[cmd sys-name sys]] [sys-name [cmd sys]]) (remove nil? (map lookup systems))))))

(defn default-system-lookup-order [commands]
  ; ensure, that general is always last
  (conj (remove #{:general} (keys commands)) :general))

(defn resolve-command
  ([commands command] (resolve-command commands command (default-system-lookup-order commands)))
  ([commands command system-lookup-order]
   ((fn inner [command alias-chain]
      (let [cmd-sym             (symbol command)
            command-sys-pairs   (lookup-command commands cmd-sym)
            [cmd-info sys]      (first (remove nil? (map command-sys-pairs system-lookup-order)))]
        (when cmd-info
          (if (= (:type cmd-info) :alias)
            (inner (:command cmd-info) (conj alias-chain command))
            (when-let [function (ns-resolve (:namespace sys) cmd-sym)]
              [function cmd-info (conj alias-chain command)])))))

    command [])))

(def env (atom {:commands        (load-commands)
                :resolve-command resolve-command
                :reload-commands (fn []
                                   (swap! env
                                          (fn [env]
                                            (assoc env :commands (load-commands)))))}))

(defn check-num-args [cmd-info args f k]
  (if (k cmd-info)
    (f (count args) (k cmd-info))
    true))

(defn select-relevant [m ks]
  (if ks (select-keys m ks) {}))

(defn add-data [cmd-info context]
  (if (:data cmd-info) (conj context (:data cmd-info)) context))

(defn execute-single [[command & args] context]
  (let [env @env]
    (if-let [[function cmd-info] (resolve-command (:commands env)
                                                  command
                                                  (list (:system context) :general))]
      (cond
        (not (user/has-sufficient-rights (:authorid context) (:min-role cmd-info)))
        {:error (str (:username context) " has insufficient rights")}

        (and (check-num-args cmd-info args >= :min-args)
             (check-num-args cmd-info args <= :max-args))
        (function args (add-data cmd-info context) (select-keys env (:needs cmd-info)))

        :else {:error  (str "Wrong number of args for " command ": " args)})
      {:error (str "No command called " command)})))

(defn add-args [cmd piped-args]
  (if (nil? piped-args)
    cmd
    (conj cmd piped-args)))

(defn execute [commands {:keys [system username authorid] :as context}]
  (log/info system "command by" username "(" authorid "):" commands)
  (reduce (fn [piped-args cmd]
            (let [command (add-args cmd piped-args)
                  result  (execute-single command context)]
              (if (:error result)
                (reduced result)
                result)))
          nil
          commands))

(defn dispatcher [command-ch]
  (loop []
    (when-let [{:keys [commands authorid reply] :as context} (a/<!! command-ch)]
      (when-not (and (= (first (first commands)) "quit")
                     (user/has-sufficient-rights authorid :admin))
        (future
          (let [result (execute commands context)]
            (if (:error result)
              (reply (:error result))
              (reply result))))
        (recur))))

  (log/info "quitting dispatcher"))
