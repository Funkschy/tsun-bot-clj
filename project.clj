(defproject tsunbot2 "0.1.0-SNAPSHOT"
  :description "Tsundere Chatbot"
  :url "https://github.com/funkschy/tsun-bot-clj"
  :license {:name "MIT"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/tools.logging "1.2.3"]
                 [org.clojure/data.json "2.4.0"]
                 [org.clojure/core.async "1.5.648"]
                 [org.suskalo/discljord "1.3.0" :exclusions [org.clojure/clojure
                                                             org.clojure/tools.logging
                                                             org.clojure/data.json
                                                             org.clojure/core.async]]
                 [org.xerial/sqlite-jdbc "3.36.0"]]
  :jvm-opts ["-Dclojure.tools.logging.factory=tsunbot.logging/logger-factory"]
  :main ^:skip-aot tsunbot.core
  :repl-options {:init-ns tsunbot.core}
  :profiles {:uberjar {:aot :all}})
