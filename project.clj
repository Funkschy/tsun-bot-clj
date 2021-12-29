(defproject tsunbot2 "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "https://github.com/funkschy/tsun-bot-clj"
  :license {:name "MIT"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.suskalo/discljord "1.3.0"]
                 [org.clojure/data.json "2.4.0"]
                 [clj-http "3.12.3"]]
  :main tsunbot.core
  :aot [tsunbot.core]
  :repl-options {:init-ns tsunbot.core})
