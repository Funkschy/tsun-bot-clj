(ns tsunbot.db
  (:require [clojure.tools.logging :as log])
  (:import java.sql.DriverManager))

(def connection
  (delay
    (log/info "creating database connection")
    (let [c (DriverManager/getConnection "jdbc:sqlite:bot.db")
          s (.createStatement c)]
      (.setQueryTimeout s 30)
      (.executeUpdate s (slurp "init.sql"))
      c)))

(defn disconnect! []
  (.close @connection))

(def setter {java.lang.Long   (memfn setLong idx value)
             java.lang.String (memfn setString idx value)})

(defn exec-query [statement & args]
  (let [s (.prepareStatement @connection statement)]
    (try
      (log/info "Executing" statement "with args:" args)
      (doseq [[i a] (map vector (range) args)] ((setter (type a)) s (inc i) a))
      (resultset-seq (.executeQuery s))
      (finally (.close s)))))

(defn exec-update [statement & args]
  (let [s (.prepareStatement @connection statement)]
    (try
      (log/info "Executing" statement "with args:" args)
      (doseq [[i a] (map vector (range) args)] ((setter (type a)) s (inc i) a))
      (.executeUpdate s)
      (finally (.close s)))))
