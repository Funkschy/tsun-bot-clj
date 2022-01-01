(ns tsunbot.logging
  (:require [clojure.string :as str]
            [clojure.tools.logging.impl :as log]
            [clojure.edn :as edn])
  (:import java.util.logging.Logger
           java.util.logging.FileHandler
           java.util.logging.Formatter
           java.util.logging.SimpleFormatter
           java.time.LocalDateTime
           java.time.Instant
           java.time.ZoneId
           java.time.format.DateTimeFormatter))

(defn millis-to-localdate [millis]
  (-> (Instant/ofEpochMilli millis) (.atZone (ZoneId/systemDefault)) (.toLocalDateTime)))

(defn file-handler [filename limit cnt append]
  (FileHandler. filename limit cnt append))

(defn date-time-formatter [fmt-string]
  (DateTimeFormatter/ofPattern fmt-string))

(defn fmt-timestamp [formatter millis]
  (-> formatter (.format (millis-to-localdate millis))))

(defn format-record [time-formatter level millis message]
  (str level
       " ("
       (fmt-timestamp time-formatter millis)
       ")"
       ": "
       message
       \newline))

(defn formatter [time-fmt-string]
  (let [formatter (date-time-formatter time-fmt-string)]
    (proxy [Formatter] []
      (format [record]
        (format-record formatter
                       (.getLevel record)
                       (.getMillis record)
                       (.getMessage record))))))


(defn create-logger [config]
  (let [java-levels     {:trace java.util.logging.Level/FINEST
                         :debug java.util.logging.Level/FINE
                         :info  java.util.logging.Level/INFO
                         :warn  java.util.logging.Level/WARNING
                         :error java.util.logging.Level/SEVERE
                         :fatal java.util.logging.Level/SEVERE}
        append          (boolean (:append config))
        limit           (or (:limit config) 4096)
        cnt             (or (:count config) 2)
        time-fmt-string (or (:date-time-formatter config) "yyyy-MM-dd HH:mm:ss")
        handler         (when (:file config) (file-handler (:file config) limit cnt append))]
    (when handler (.setFormatter handler (formatter time-fmt-string)))

    (memoize
      (fn [logger-name]
        (let [logger (Logger/getLogger logger-name)]
          (extend Logger
            log/Logger
            {:enabled? (fn [^Logger logger level]
                         (.isLoggable logger (get java-levels level level)))
             :write! (fn [^Logger logger level e message]
                       (let [level   (get java-levels level level)
                             message (str message)]
                         (if e
                           (.log logger level message e)
                           (.log logger level message)))) })
          (when handler
            (.addHandler logger handler)
            (.setUseParentHandlers logger false))

          (.setLevel logger (get java-levels (:level config) (:level config)))
          logger)))))

(defn logger-factory
  "Creates a java.util.logging.Logger which respects the tsunbot config. This is necessary,
  because the clojure.tools.logging library does not expose any way to configure the logger"
  []
  (let [config (:log (edn/read-string (slurp "config.edn")))
        logger (create-logger config)]
    (reify log/LoggerFactory
      (name [_] "java.util.logging")
      (get-logger [_ logger-ns]
        (logger (str logger-ns))))))